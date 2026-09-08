package com.educa.backend.quiz;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.educa.backend.common.error.ApiException;
import com.educa.backend.common.error.ResourceNotFoundException;
import com.educa.backend.course.Chapter;
import com.educa.backend.course.ChapterService;
import com.educa.backend.course.CourseService;
import com.educa.backend.enrollment.EnrollmentService;
import com.educa.backend.security.CurrentUser;
import com.educa.backend.quiz.dto.OptionRequest;
import com.educa.backend.quiz.dto.QuestionRequest;
import com.educa.backend.quiz.dto.QuizRequest;
import com.educa.backend.quiz.dto.QuizViewDto;
import com.educa.backend.quiz.dto.QuizViewDto.OptionViewDto;
import com.educa.backend.quiz.dto.QuizViewDto.QuestionViewDto;

@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final CourseService courseService;
    private final ChapterService chapterService;
    private final EnrollmentService enrollmentService;
    private final QuizUnlockService unlockService;

    public QuizService(QuizRepository quizRepository, QuestionRepository questionRepository,
                       CourseService courseService, ChapterService chapterService,
                       EnrollmentService enrollmentService, QuizUnlockService unlockService) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.courseService = courseService;
        this.chapterService = chapterService;
        this.enrollmentService = enrollmentService;
        this.unlockService = unlockService;
    }

    /**
     * Vue d'un quiz pour l'utilisateur courant : version complète pour le
     * propriétaire / ADMIN, version sans les bonnes réponses pour l'apprenant inscrit.
     * Toute la lecture (collections lazy) se fait dans cette transaction.
     */
    @Transactional(readOnly = true)
    public QuizViewDto getViewForCurrentUser(Long quizId) {
        Quiz quiz = requireQuiz(quizId);
        if (courseService.isOwnerOrAdmin(quiz.getCourseId())) {
            return toView(quiz, true);
        }
        Long userId = CurrentUser.id();
        if (!enrollmentService.isEnrolled(userId, quiz.getCourseId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Vous n'êtes pas inscrit à ce cours");
        }
        if (quiz.getType() == QuizType.FINAL_EXAM
                && !unlockService.isFinalExamUnlocked(userId, quiz.getCourseId())) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "L'examen final se débloque à 100 % de progression");
        }
        return toView(quiz, false);
    }

    // ---------- CRUD quiz (formateur propriétaire / ADMIN) ----------

    @Transactional
    public QuizViewDto createControlQuiz(Long chapterId, QuizRequest request) {
        Chapter chapter = chapterService.requireOwned(chapterId);
        if (quizRepository.existsByChapterIdAndType(chapterId, QuizType.CONTROL)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ce chapitre a déjà un contrôle");
        }
        Quiz quiz = new Quiz();
        quiz.setType(QuizType.CONTROL);
        quiz.setCourseId(chapter.getCourse().getId());
        quiz.setChapterId(chapterId);
        quiz.setTitle(request.title().trim());
        quiz.setPassThreshold(request.passThreshold() != null ? request.passThreshold() : 50);
        quiz.setMaxAttempts(request.maxAttempts());
        return toView(quizRepository.save(quiz), true);
    }

    @Transactional
    public QuizViewDto createFinalExam(Long courseId, QuizRequest request) {
        courseService.requireOwned(courseId);
        if (quizRepository.existsByCourseIdAndType(courseId, QuizType.FINAL_EXAM)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ce cours a déjà un examen final");
        }
        Quiz quiz = new Quiz();
        quiz.setType(QuizType.FINAL_EXAM);
        quiz.setCourseId(courseId);
        quiz.setTitle(request.title().trim());
        quiz.setPassThreshold(request.passThreshold() != null ? request.passThreshold() : 50);
        quiz.setMaxAttempts(request.maxAttempts() != null ? request.maxAttempts() : 3);
        return toView(quizRepository.save(quiz), true);
    }

    @Transactional
    public QuizViewDto updateQuiz(Long quizId, QuizRequest request) {
        Quiz quiz = requireOwned(quizId);
        quiz.setTitle(request.title().trim());
        if (request.passThreshold() != null) {
            quiz.setPassThreshold(request.passThreshold());
        }
        quiz.setMaxAttempts(request.maxAttempts());
        return toView(quiz, true);
    }

    @Transactional
    public void deleteQuiz(Long quizId) {
        quizRepository.delete(requireOwned(quizId));
    }

    @Transactional
    public QuizViewDto addQuestion(Long quizId, QuestionRequest request) {
        Quiz quiz = requireOwned(quizId);
        validateOptions(request);
        if (questionRepository.existsByQuizIdAndPosition(quizId, request.position())) {
            throw new ApiException(HttpStatus.CONFLICT, "Une question occupe déjà la position " + request.position());
        }
        Question question = buildQuestion(request);
        quiz.addQuestion(question);
        quizRepository.save(quiz);
        return toView(quiz, true);
    }

    @Transactional
    public QuizViewDto updateQuestion(Long questionId, QuestionRequest request) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question introuvable"));
        Quiz quiz = requireOwned(question.getQuiz().getId());
        validateOptions(request);

        question.setStatement(request.statement().trim());
        question.setType(request.type());
        question.setPoints(request.points() != null ? request.points() : 1);
        question.setPosition(request.position());
        question.getOptions().clear();
        for (OptionRequest option : request.options()) {
            question.addOption(buildOption(option));
        }
        questionRepository.save(question);
        return toView(quiz, true);
    }

    @Transactional
    public void deleteQuestion(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question introuvable"));
        requireOwned(question.getQuiz().getId());
        questionRepository.delete(question);
    }

    // ---------- lecture ----------

    @Transactional(readOnly = true)
    public Quiz requireOwned(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz introuvable"));
        courseService.requireOwned(quiz.getCourseId());
        return quiz;
    }

    @Transactional(readOnly = true)
    public Quiz requireQuiz(Long quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz introuvable"));
    }

    /** Liste des quiz d'un cours (contrôles + examen final) pour l'apprenant inscrit ou le propriétaire. */
    @Transactional(readOnly = true)
    public com.educa.backend.quiz.dto.CourseQuizzesDto listForCourse(Long courseId) {
        if (!courseService.isOwnerOrAdmin(courseId)
                && !enrollmentService.isEnrolled(CurrentUser.id(), courseId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Vous n'êtes pas inscrit à ce cours");
        }
        var controls = quizRepository.findByCourseIdAndTypeOrderByIdAsc(courseId, QuizType.CONTROL).stream()
                .map(this::toRef)
                .toList();
        var finalExam = quizRepository.findByCourseIdAndType(courseId, QuizType.FINAL_EXAM)
                .map(this::toRef)
                .orElse(null);
        return new com.educa.backend.quiz.dto.CourseQuizzesDto(controls, finalExam);
    }

    private com.educa.backend.quiz.dto.CourseQuizzesDto.QuizRefDto toRef(Quiz quiz) {
        return new com.educa.backend.quiz.dto.CourseQuizzesDto.QuizRefDto(quiz.getId(), quiz.getChapterId(),
                quiz.getType().name(), quiz.getTitle(), quiz.getQuestions().size(), quiz.getMaxAttempts());
    }

    public QuizViewDto toView(Quiz quiz, boolean answersVisible) {
        List<QuestionViewDto> questions = quiz.getQuestions().stream()
                .map(q -> new QuestionViewDto(q.getId(), q.getStatement(), q.getType().name(), q.getPoints(),
                        q.getPosition(),
                        q.getOptions().stream()
                                .map(o -> new OptionViewDto(o.getId(), o.getLabel(), o.getPosition(),
                                        answersVisible ? o.isCorrect() : null))
                                .toList()))
                .toList();
        return new QuizViewDto(quiz.getId(), quiz.getType().name(), quiz.getTitle(), quiz.getPassThreshold(),
                quiz.getMaxAttempts(), answersVisible, questions);
    }

    // ---------- privé ----------

    private void validateOptions(QuestionRequest request) {
        List<OptionRequest> options = request.options();
        long correct = options.stream().filter(OptionRequest::correct).count();
        if (options.size() < 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Au moins 2 options sont requises");
        }
        if (correct < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Au moins une option correcte est requise");
        }
        if (request.type() == QuestionType.TRUE_FALSE && options.size() != 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Une question Vrai/Faux doit avoir exactement 2 options");
        }
        if (request.type() == QuestionType.SINGLE_CHOICE && correct != 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Une question à choix unique doit avoir exactement 1 réponse");
        }
    }

    private Question buildQuestion(QuestionRequest request) {
        Question question = new Question();
        question.setStatement(request.statement().trim());
        question.setType(request.type());
        question.setPoints(request.points() != null ? request.points() : 1);
        question.setPosition(request.position());
        for (OptionRequest option : request.options()) {
            question.addOption(buildOption(option));
        }
        return question;
    }

    private AnswerOption buildOption(OptionRequest request) {
        AnswerOption option = new AnswerOption();
        option.setLabel(request.label().trim());
        option.setCorrect(request.correct());
        option.setPosition(request.position());
        return option;
    }
}
