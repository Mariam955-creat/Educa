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
import com.educa.backend.user.UserService;

@Service
public class RatingService {

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
        boolean canRate = userId != null && enrollmentService.isEnrolled(userId, courseId);
        return new CourseRatingDto(stats != null ? stats.average() : null, stats != null ? stats.count() : 0,
                mine != null ? (int) mine.getStars() : null, mine != null ? mine.getComment() : null, canRate);
    }

    /**
     * Crée ou remplace la note (et l'avis écrit facultatif) de l'apprenant : réservé aux inscrits, un avis
     * doit venir de quelqu'un qui suit le cours.
     */
    @Transactional
    public CourseRatingDto rate(Long courseId, Long userId, int stars, String comment) {
        courseService.requireCourse(courseId);
        if (!enrollmentService.isEnrolled(userId, courseId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Seuls les apprenants inscrits peuvent noter ce cours");
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
