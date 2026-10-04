package com.educa.backend.admin;

import java.time.Duration;
import java.time.Instant;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.educa.backend.certificate.CertificateService;
import com.educa.backend.course.CourseRatingProvider.RatingStats;
import com.educa.backend.course.CourseService;
import com.educa.backend.enrollment.EnrollmentService;
import com.educa.backend.payment.PaymentService;
import com.educa.backend.rating.RatingService;
import com.educa.backend.user.UserService;

/** Tableau de bord admin : indicateurs globaux de la plateforme. */
@RestController
@RequestMapping("/api/v1/admin/stats")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatsController {

    private static final Duration RECENT = Duration.ofDays(30);

    private final UserService userService;
    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final PaymentService paymentService;
    private final CertificateService certificateService;
    private final RatingService ratingService;

    public AdminStatsController(UserService userService, CourseService courseService,
                                EnrollmentService enrollmentService, PaymentService paymentService,
                                CertificateService certificateService, RatingService ratingService) {
        this.userService = userService;
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
        this.paymentService = paymentService;
        this.certificateService = certificateService;
        this.ratingService = ratingService;
    }

    @GetMapping
    public AdminStatsDto stats() {
        Instant since = Instant.now().minus(RECENT);
        UserService.UserStats users = userService.stats(since);
        long[] courses = courseService.courseCounts();
        long[] enrollments = enrollmentService.enrollmentCounts(since);
        PaymentService.RevenueStats revenue = paymentService.revenueStats(since);
        RatingStats ratings = ratingService.globalStats();
        return new AdminStatsDto(
                new AdminStatsDto.Users(users.total(), users.learners(), users.instructors(), users.admins(),
                        users.disabled(), users.recent()),
                new AdminStatsDto.Courses(courses[0], courses[1], courses[0] - courses[1]),
                new AdminStatsDto.Enrollments(enrollments[0], enrollments[1]),
                new AdminStatsDto.Revenue(revenue.total(), revenue.sales(), revenue.recent(), revenue.recentSales(),
                        revenue.currency()),
                certificateService.count(),
                new AdminStatsDto.Reviews(ratings.count(), ratings.count() == 0 ? null : ratings.average()));
    }
}
