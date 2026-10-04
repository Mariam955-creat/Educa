package com.educa.backend.rating;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.educa.backend.common.error.ApiException;
import com.educa.backend.common.error.ResourceNotFoundException;
import com.educa.backend.course.Course;
import com.educa.backend.course.CourseRatingProvider.RatingStats;
import com.educa.backend.course.CourseService;
import com.educa.backend.enrollment.EnrollmentService;
import com.educa.backend.rating.dto.AdminReviewDto;
import com.educa.backend.rating.dto.CourseRatingDto;
import com.educa.backend.rating.dto.CourseReviewDto;
import com.educa.backend.rating.dto.MyReviewDto;
import com.educa.backend.user.UserService;

@Service
public class RatingService {

    /** Progression minimale (en % des contenus terminés) pour noter un cours : un avis doit venir de quelqu'un qui l'a réellement suivi. */
    static final int REQUIRED_PROGRESS_PERCENT = 70;

    private final CourseRatingRepository ratingRepository;
    private final RatingCourseStatsProvider statsProvider;
    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final UserService userService;

    public RatingService(CourseRatingRepository ratingRepository, RatingCourseStatsProvider statsProvider,
                         CourseService courseService, EnrollmentService enrollmentService, UserService userService) {
        this.ratingRepository = ratingRepository;
        this.statsProvider = statsProvider;
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
        this.userService = userService;
    }

    /** @param userId utilisateur courant, {@code null} s'il est anonyme */
    @Transactional(readOnly = true)
    public CourseRatingDto get(Long courseId, Long userId) {
        courseService.requireCourse(courseId);
        RatingStats stats = statsProvider.ratingStats(List.of(courseId)).get(courseId);
        CourseRating mine = userId == null ? null
                : ratingRepository.findByCourseIdAndUserId(courseId, userId).orElse(null);
        boolean enrolled = userId != null && enrollmentService.isEnrolled(userId, courseId);
        int progress = enrolled ? enrollmentService.progressPercent(userId, courseId) : 0;
        return new CourseRatingDto(stats != null ? stats.average() : null, stats != null ? stats.count() : 0,
                mine != null ? (int) mine.getStars() : null, mine != null ? mine.getComment() : null,
                enrolled, progress, REQUIRED_PROGRESS_PERCENT, enrolled && progress >= REQUIRED_PROGRESS_PERCENT);
    }

    /**
     * Crée ou remplace la note (et l'avis écrit facultatif) de l'apprenant : réservé aux inscrits ayant suivi
     * au moins {@value #REQUIRED_PROGRESS_PERCENT} % du cours.
     */
    @Transactional
    public CourseRatingDto rate(Long courseId, Long userId, int stars, String comment) {
        courseService.requireCourse(courseId);
        if (!enrollmentService.isEnrolled(userId, courseId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Seuls les apprenants inscrits peuvent noter ce cours");
        }
        int progress = enrollmentService.progressPercent(userId, courseId);
        if (progress < REQUIRED_PROGRESS_PERCENT) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Suivez au moins " + REQUIRED_PROGRESS_PERCENT
                    + " % du cours pour le noter (progression actuelle : " + progress + " %)");
        }
        CourseRating rating = ratingRepository.findByCourseIdAndUserId(courseId, userId).orElseGet(() -> {
            CourseRating created = new CourseRating();
            created.setCourseId(courseId);
            created.setUserId(userId);
            return created;
        });
        rating.setStars((short) stars);
        rating.setComment(StringUtils.hasText(comment) ? comment.trim() : null);
        ratingRepository.saveAndFlush(rating);
        return get(courseId, userId);
    }

    /** Registre de modération : toutes les notes (avec ou sans texte), de la plus récente à la plus ancienne. */
    @Transactional(readOnly = true)
    public Page<AdminReviewDto> registry(Pageable pageable) {
        Map<Long, Course> courses = new HashMap<>();
        return ratingRepository.findAllByOrderByUpdatedAtDesc(pageable).map(r -> {
            Course course = courses.computeIfAbsent(r.getCourseId(), courseService::requireCourse);
            return new AdminReviewDto(r.getId(), course.getId(), course.getTitle(), course.getSlug(),
                    userService.displayNameById(r.getUserId()), r.getStars(), r.getComment(), r.getUpdatedAt());
        });
    }

    /** Avis de l'utilisateur, du plus récent au plus ancien. */
    @Transactional(readOnly = true)
    public List<MyReviewDto> mine(Long userId) {
        Map<Long, Course> courses = new HashMap<>();
        return ratingRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream().map(r -> {
            Course course = courses.computeIfAbsent(r.getCourseId(), courseService::requireCourse);
            return new MyReviewDto(r.getId(), course.getId(), course.getTitle(), course.getSlug(), r.getStars(),
                    r.getComment(), r.getUpdatedAt());
        }).toList();
    }

    /** L'utilisateur retire son propre avis ; celui d'un autre est traité comme inexistant (pas de fuite d'existence). */
    @Transactional
    public void deleteMine(Long ratingId, Long userId) {
        CourseRating rating = ratingRepository.findById(ratingId)
                .filter(r -> r.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Avis introuvable"));
        ratingRepository.delete(rating);
    }

    /** Avis reçus par les cours d'un formateur, du plus récent au plus ancien. */
    @Transactional(readOnly = true)
    public List<AdminReviewDto> receivedByInstructor(Long instructorId) {
        List<Long> courseIds = courseService.courseIdsByInstructor(instructorId);
        if (courseIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Course> courses = new HashMap<>();
        return ratingRepository.findByCourseIdInOrderByUpdatedAtDesc(courseIds).stream().map(r -> {
            Course course = courses.computeIfAbsent(r.getCourseId(), courseService::requireCourse);
            return new AdminReviewDto(r.getId(), course.getId(), course.getTitle(), course.getSlug(),
                    userService.displayNameById(r.getUserId()), r.getStars(), r.getComment(), r.getUpdatedAt());
        }).toList();
    }

    /** Suppression par un administrateur (modération). */
    @Transactional
    public void delete(Long ratingId) {
        CourseRating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new ResourceNotFoundException("Avis introuvable"));
        ratingRepository.delete(rating);
    }

    /** Avis écrits d'un cours publié (ou visible par son propriétaire/ADMIN), du plus récent au plus ancien. */
    @Transactional(readOnly = true)
    public Page<CourseReviewDto> reviews(Long courseId, Pageable pageable) {
        Course course = courseService.requireCourse(courseId);
        if (!course.isPublished() && !courseService.isOwnerOrAdmin(courseId)) {
            throw new ResourceNotFoundException("Cours introuvable");
        }
        return ratingRepository.findByCourseIdAndCommentIsNotNullOrderByUpdatedAtDesc(courseId, pageable)
                .map(r -> new CourseReviewDto(r.getId(), userService.displayNameById(r.getUserId()), r.getStars(),
                        r.getComment(), r.getUpdatedAt()));
    }
}
