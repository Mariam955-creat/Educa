package com.educa.backend.enrollment;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.educa.backend.common.error.ApiException;
import com.educa.backend.course.Course;
import com.educa.backend.course.ContentService;
import com.educa.backend.course.CourseService;
import com.educa.backend.course.dto.CourseSummaryDto;
import com.educa.backend.enrollment.dto.CourseProgressDto;
import com.educa.backend.enrollment.dto.EnrollmentDto;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final ProgressRepository progressRepository;
    private final CourseService courseService;
    private final ContentService contentService;

    public EnrollmentService(EnrollmentRepository enrollmentRepository, ProgressRepository progressRepository,
                             CourseService courseService, ContentService contentService) {
        this.enrollmentRepository = enrollmentRepository;
        this.progressRepository = progressRepository;
        this.courseService = courseService;
        this.contentService = contentService;
    }

    @Transactional
    public EnrollmentDto enroll(Long userId, Long courseId) {
        Course course = courseService.requireCourse(courseId);
        if (!course.isPublished()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Ce cours n'est pas publié");
        }
        enrollmentRepository.findByUserIdAndCourseId(userId, courseId).ifPresent(existing -> {
            throw new ApiException(HttpStatus.CONFLICT, "Vous êtes déjà inscrit à ce cours");
        });
        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(userId);
        enrollment.setCourseId(courseId);
        enrollmentRepository.save(enrollment);
        return toDto(enrollment);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentDto> myEnrollments(Long userId) {
        return enrollmentRepository.findByUserIdOrderByEnrolledAtDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public CourseProgressDto completeContent(Long userId, Long contentId) {
        Long courseId = contentService.courseIdOf(contentId);
        Enrollment enrollment = requireActiveEnrollment(userId, courseId);
        if (!progressRepository.existsByEnrollmentIdAndContentId(enrollment.getId(), contentId)) {
            Progress progress = new Progress();
            progress.setEnrollmentId(enrollment.getId());
            progress.setContentId(contentId);
            progressRepository.save(progress);
        }
        return progressOf(enrollment, courseId);
    }

    @Transactional(readOnly = true)
    public CourseProgressDto courseProgress(Long userId, Long courseId) {
        Enrollment enrollment = requireActiveEnrollment(userId, courseId);
        return progressOf(enrollment, courseId);
    }

    @Transactional(readOnly = true)
    public boolean isEnrolled(Long userId, Long courseId) {
        return enrollmentRepository.existsByUserIdAndCourseIdAndStatus(userId, courseId, EnrollmentStatus.ACTIVE)
                || enrollmentRepository.existsByUserIdAndCourseIdAndStatus(userId, courseId, EnrollmentStatus.COMPLETED);
    }

    @Transactional(readOnly = true)
    public boolean contentsFullyCompleted(Long userId, Long courseId) {
        long total = courseService.contentCount(courseId);
        if (total == 0) {
            return false;
        }
        return enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .map(e -> progressRepository.countByEnrollmentId(e.getId()) >= total)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public int progressPercent(Long userId, Long courseId) {
        long total = courseService.contentCount(courseId);
        return enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .map(e -> percent(progressRepository.countByEnrollmentId(e.getId()), total))
                .orElse(0);
    }

    @Transactional(readOnly = true)
    public List<Long> enrolledUserIds(Long courseId) {
        return enrollmentRepository.findByCourseId(courseId).stream().map(Enrollment::getUserId).toList();
    }

    @Transactional
    public void markCompleted(Long userId, Long courseId) {
        enrollmentRepository.findByUserIdAndCourseId(userId, courseId).ifPresent(e -> {
            if (e.getStatus() != EnrollmentStatus.COMPLETED) {
                e.setStatus(EnrollmentStatus.COMPLETED);
                e.setCompletedAt(Instant.now());
                enrollmentRepository.save(e);
            }
        });
    }

    // ---------- privé ----------

    private Enrollment requireActiveEnrollment(Long userId, Long courseId) {
        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Vous n'êtes pas inscrit à ce cours"));
        if (enrollment.getStatus() == EnrollmentStatus.CANCELLED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Inscription annulée");
        }
        return enrollment;
    }

    private CourseProgressDto progressOf(Enrollment enrollment, Long courseId) {
        long total = courseService.contentCount(courseId);
        List<Long> completedIds = progressRepository.findByEnrollmentId(enrollment.getId()).stream()
                .map(Progress::getContentId)
                .toList();
        return new CourseProgressDto(courseId, (int) total, completedIds.size(),
                percent(completedIds.size(), total), completedIds);
    }

    private EnrollmentDto toDto(Enrollment enrollment) {
        CourseSummaryDto course = courseService.summary(enrollment.getCourseId());
        long total = courseService.contentCount(enrollment.getCourseId());
        long completed = progressRepository.countByEnrollmentId(enrollment.getId());
        Instant enrolledAt = enrollment.getEnrolledAt();
        return new EnrollmentDto(course.id(), course.slug(), course.title(), enrollment.getStatus().name(),
                (int) total, (int) completed, percent(completed, total), enrolledAt);
    }

    private static int percent(long completed, long total) {
        return total == 0 ? 0 : (int) Math.round(100.0 * completed / total);
    }
}
