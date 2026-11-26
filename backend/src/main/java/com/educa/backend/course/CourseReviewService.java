package com.educa.backend.course;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.educa.backend.common.error.ApiException;
import com.educa.backend.common.error.ConflictException;
import com.educa.backend.config.EducaProperties;
import com.educa.backend.course.dto.CourseSummaryDto;
import com.educa.backend.notification.EmailService;
import com.educa.backend.notification.NotificationService;
import com.educa.backend.notification.NotificationType;
import com.educa.backend.user.UserService;
import com.educa.backend.user.dto.UserDto;

/**
 * Validation des cours par l'administration : le formateur soumet, les admins sont notifiés ; l'admin valide (le
 * cours est publié) ou refuse avec un motif (le formateur corrige puis resoumet). Le formateur est prévenu de la
 * décision par une notification et par email.
 */
@Service
public class CourseReviewService {

    private final CourseRepository courseRepository;
    private final CourseService courseService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final String publicBaseUrl;

    public CourseReviewService(CourseRepository courseRepository, CourseService courseService, UserService userService,
            NotificationService notificationService, EmailService emailService, EducaProperties properties) {
        this.courseRepository = courseRepository;
        this.courseService = courseService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.publicBaseUrl = properties.publicBaseUrl();
    }

    /**
     * Soumet un cours pour validation (premier envoi, ou renvoi après un refus). Refusé (409) s'il est déjà
     * publié ou en attente ; (400) s'il n'a aucun chapitre.
     */
    @Transactional
    public CourseSummaryDto submit(Long courseId) {
        Course course = courseService.requireOwned(courseId);
        if (course.isPublished() || course.getReviewStatus() == CourseReviewStatus.APPROVED) {
            throw new ConflictException("Ce cours est déjà validé : vous pouvez le publier directement");
        }
        if (course.getReviewStatus() == CourseReviewStatus.PENDING) {
            throw new ConflictException("Ce cours est déjà en attente de validation");
        }
        if (course.getChapters().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ajoutez au moins un chapitre avant de soumettre le cours");
        }
        course.setReviewStatus(CourseReviewStatus.PENDING);
        course.setSubmittedAt(Instant.now());
        String instructor = userService.displayNameById(course.getInstructorId());
        for (Long adminId : userService.activeAdminIds()) {
            notificationService.notify(adminId, NotificationType.COURSE_SUBMITTED, course.getTitle(), instructor,
                    "/admin/course-reviews");
        }
        return courseService.summary(course.getId());
    }

    @Transactional(readOnly = true)
    public Page<CourseSummaryDto> pending(Pageable pageable) {
        return courseRepository.findByReviewStatusAndDeletedAtIsNull(CourseReviewStatus.PENDING, pageable)
                .map(course -> courseService.summary(course.getId()));
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return courseRepository.countByReviewStatusAndDeletedAtIsNull(CourseReviewStatus.PENDING);
    }

    /** Valide le cours : il est publié ; le formateur est notifié et reçoit un email. */
    @Transactional
    public CourseSummaryDto approve(Long courseId) {
        Course course = loadPending(courseId);
        course.setReviewStatus(CourseReviewStatus.APPROVED);
        course.setReviewReason(null);
        course.setReviewedAt(Instant.now());
        course.setPublished(true);
        String link = "/courses/" + course.getSlug();
        notificationService.notify(course.getInstructorId(), NotificationType.COURSE_APPROVED, course.getTitle(), null,
                link);
        UserDto instructor = userService.getById(course.getInstructorId());
        String title = course.getTitle();
        afterCommit(() -> emailService.send(instructor.email(), instructor.preferredLanguage(), "courseApproved",
                instructor.fullName(), title, publicBaseUrl + link));
        return courseService.summary(course.getId());
    }

    /** Refuse le cours avec un motif : il reste non publié ; le formateur est notifié et reçoit un email. */
    @Transactional
    public CourseSummaryDto reject(Long courseId, String reason) {
        Course course = loadPending(courseId);
        String motive = reason.trim();
        course.setReviewStatus(CourseReviewStatus.REJECTED);
        course.setReviewReason(motive);
        course.setReviewedAt(Instant.now());
        String link = "/instructor/courses/" + course.getSlug() + "/edit";
        notificationService.notify(course.getInstructorId(), NotificationType.COURSE_REJECTED, course.getTitle(),
                motive, link);
        UserDto instructor = userService.getById(course.getInstructorId());
        String title = course.getTitle();
        afterCommit(() -> emailService.send(instructor.email(), instructor.preferredLanguage(), "courseRejected",
                instructor.fullName(), title, motive, publicBaseUrl + link));
        return courseService.summary(course.getId());
    }

    private Course loadPending(Long courseId) {
        Course course = courseService.requireCourse(courseId);
        if (course.getReviewStatus() != CourseReviewStatus.PENDING) {
            throw new ConflictException("Ce cours n'est pas en attente de validation");
        }
        return course;
    }

    /** L'email ne part qu'une fois la décision enregistrée (jamais pour une transaction annulée). */
    private static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
