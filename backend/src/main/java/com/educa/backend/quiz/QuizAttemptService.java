package com.educa.backend.quiz;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.educa.backend.certificate.CertificateService;
import com.educa.backend.common.error.ApiException;
import com.educa.backend.course.Course;
import com.educa.backend.course.CourseService;
import com.educa.backend.enrollment.EnrollmentService;
import com.educa.backend.quiz.GradingService.GradeResult;
import com.educa.backend.quiz.dto.AttemptResultDto;
import com.educa.backend.quiz.dto.AttemptSubmission;
import com.educa.backend.quiz.dto.AttemptSummaryDto;
import com.educa.backend.quiz.dto.CourseGradeDto;
import com.educa.backend.quiz.dto.CourseGradeDto.ControlScoreDto;
import com.educa.backend.quiz.dto.LearnerResultDto;
import com.educa.backend.user.UserService;

@Service
public class QuizAttemptService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final GradingService gradingService;
    private final QuizService quizService;
    private final QuizUnlockService unlockService;
    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final CertificateService certificateService;
    private final UserService userService;

    public QuizAttemptService(QuizRepository quizRepository, QuizAttemptRepository attemptRepository,
                              AttemptAnswerRepository attemptAnswerRepository, GradingService gradingService,
                              QuizService quizService, QuizUnlockService unlockService, CourseService courseService,
                              EnrollmentService enrollmentService, CertificateService certificateService,
                              UserService userService) {
        this.quizRepository = quizRepository;
        this.attemptRepository = attemptRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.gradingService = gradingService;
        this.quizService = quizService;
        this.unlockService = unlockService;
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
        this.certificateService = certificateService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<LearnerResultDto> courseResults(Long courseId) {
        Course course = courseService.requireCourse(courseId);
        return enrollmentService.enrolledUserIds(courseId).stream().map(uid -> {
            BigDecimal ctrl = controlsAverage(uid, courseId);
            BigDecimal exam = bestFinalExamScore(uid, courseId);
            BigDecimal grade = exam == null ? null : weightedFinalGrade(course, ctrl, exam);
            boolean certified = certificateService.certificateIdFor(uid, courseId) != null;
            return new LearnerResultDto(uid, userService.displayNameById(uid), ctrl, exam, grade, certified);
        }).toList();
    }

    @Transactional
    public AttemptResultDto submit(Long userId, Long quizId, AttemptSubmission submission) {
        Quiz quiz = quizService.requireQuiz(quizId);

        if (!enrollmentService.isEnrolled(userId, quiz.getCourseId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Vous n'êtes pas inscrit à ce cours");
        }
        if (quiz.getType() == QuizType.FINAL_EXAM
                && !unlockService.isFinalExamUnlocked(userId, quiz.getCourseId())) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "L'examen final se débloque à 100 % de progression (contenus vus + tous les contrôles tentés)");
        }
        if (quiz.getMaxAttempts() != null
                && attemptRepository.countByUserIdAndQuizId(userId, quizId) >= quiz.getMaxAttempts()) {
            throw new ApiException(HttpStatus.CONFLICT, "Nombre maximum de tentatives atteint");
        }

        GradeResult result = gradingService.grade(quiz, submission.answers());
        Instant now = Instant.now();

        QuizAttempt attempt = new QuizAttempt();
        attempt.setUserId(userId);
        attempt.setQuizId(quizId);
        attempt.setScore(result.score());
        attempt.setPassed(result.score().compareTo(BigDecimal.valueOf(quiz.getPassThreshold())) >= 0);
        attempt.setStartedAt(now);
        attempt.setSubmittedAt(now);
        attemptRepository.save(attempt);

        result.answers().forEach(answer -> {
            answer.setAttemptId(attempt.getId());
            attemptAnswerRepository.save(answer);
        });

        if (quiz.getType() != QuizType.FINAL_EXAM) {
            return new AttemptResultDto(attempt.getId(), result.score(), attempt.isPassed(),
                    result.correctCount(), result.totalQuestions(), null, null, null);
        }

        // Examen final : note pondérée + certificat éventuel
        Course course = courseService.requireCourse(quiz.getCourseId());
        BigDecimal controlsAverage = controlsAverage(userId, quiz.getCourseId());
        BigDecimal examBest = bestScore(userId, quizId); // inclut la tentative qui vient d'être enregistrée
        BigDecimal finalGrade = weightedFinalGrade(course, controlsAverage, examBest);

        Long certificateId = null;
        if (finalGrade.compareTo(BigDecimal.valueOf(course.getPassThreshold())) >= 0) {
            certificateId = certificateService.issueIfAbsent(userId, quiz.getCourseId(), attempt.getId(),
                    controlsAverage, examBest, finalGrade);
            enrollmentService.markCompleted(userId, quiz.getCourseId());
        }

        return new AttemptResultDto(attempt.getId(), result.score(), attempt.isPassed(),
                result.correctCount(), result.totalQuestions(), controlsAverage, finalGrade, certificateId);
    }

    @Transactional(readOnly = true)
    public List<AttemptSummaryDto> myAttempts(Long userId, Long quizId) {
        return attemptRepository.findByUserIdAndQuizIdOrderBySubmittedAtDesc(userId, quizId).stream()
                .map(a -> new AttemptSummaryDto(a.getId(), a.getScore(), a.isPassed(), a.getSubmittedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseGradeDto courseGrade(Long userId, Long courseId) {
        Course course = courseService.requireCourse(courseId);
        List<Quiz> controls = quizRepository.findByCourseIdAndTypeOrderByIdAsc(courseId, QuizType.CONTROL);

        List<ControlScoreDto> controlScores = controls.stream().map(q -> {
            BigDecimal best = bestScore(userId, q.getId());
            long attempts = attemptRepository.countByUserIdAndQuizId(userId, q.getId());
            return new ControlScoreDto(q.getId(), q.getChapterId(), best == null ? BigDecimal.ZERO : best, attempts);
        }).toList();

        BigDecimal controlsAverage = controlScores.isEmpty() ? null
                : controlScores.stream().map(ControlScoreDto::bestScore)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(controlScores.size()), 2, RoundingMode.HALF_UP);

        Quiz finalExam = quizRepository.findByCourseIdAndType(courseId, QuizType.FINAL_EXAM).orElse(null);
        BigDecimal examBest = finalExam == null ? null : bestScore(userId, finalExam.getId());
        BigDecimal finalGrade = examBest == null ? null : weightedFinalGrade(course, controlsAverage, examBest);

        Long certificateId = certificateService.certificateIdFor(userId, courseId);

        return new CourseGradeDto(courseId, enrollmentService.progressPercent(userId, courseId),
                unlockService.isFinalExamUnlocked(userId, courseId), controlsAverage, controlScores, examBest, finalGrade,
                course.getPassThreshold(), certificateId);
    }

    private BigDecimal bestFinalExamScore(Long userId, Long courseId) {
        return quizRepository.findByCourseIdAndType(courseId, QuizType.FINAL_EXAM)
                .map(q -> bestScore(userId, q.getId()))
                .orElse(null);
    }

    // ---------- privé ----------

    private BigDecimal controlsAverage(Long userId, Long courseId) {
        List<Quiz> controls = quizRepository.findByCourseIdAndTypeOrderByIdAsc(courseId, QuizType.CONTROL);
        if (controls.isEmpty()) {
            return null;
        }
        BigDecimal sum = controls.stream()
                .map(q -> {
                    BigDecimal best = bestScore(userId, q.getId());
                    return best == null ? BigDecimal.ZERO : best;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(controls.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal bestScore(Long userId, Long quizId) {
        return attemptRepository.findByUserIdAndQuizIdOrderBySubmittedAtDesc(userId, quizId).stream()
                .map(QuizAttempt::getScore)
                .max(BigDecimal::compareTo)
                .orElse(null);
    }

    private BigDecimal weightedFinalGrade(Course course, BigDecimal controlsAverage, BigDecimal examBest) {
        BigDecimal exam = examBest == null ? BigDecimal.ZERO : examBest;
        if (controlsAverage == null) {
            return exam.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal cw = BigDecimal.valueOf(course.getControlWeight());
        BigDecimal ew = BigDecimal.valueOf(course.getExamWeight());
        return controlsAverage.multiply(cw)
                .add(exam.multiply(ew))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
