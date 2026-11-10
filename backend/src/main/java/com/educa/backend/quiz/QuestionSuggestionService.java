package com.educa.backend.quiz;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import com.educa.backend.ai.GeneratedQuiz;
import com.educa.backend.ai.QuizGenerator;
import com.educa.backend.course.CourseService;
import com.educa.backend.quiz.dto.OptionRequest;
import com.educa.backend.quiz.dto.QuestionRequest;

/**
 * Suggestions de questions générées par l'IA pour un quiz (tâche 4.11). Rien n'est enregistré : les suggestions
 * ont la forme d'un {@link QuestionRequest} prêt à envoyer à {@code POST /quizzes/{id}/questions}, une fois relu
 * par le formateur. Les suggestions qui ne respectent pas les règles d'une question (voir
 * {@code QuizService.validateOptions}) sont écartées.
 */
@Service
public class QuestionSuggestionService {

    /** Contenu envoyé au modèle : plus large que pour le chatbot, un quiz couvre tout un chapitre ou un cours. */
    static final int MAX_CONTEXT_CHARS = 12_000;
    static final int MAX_COUNT = 10;
    private static final int MAX_OPTIONS = 6;
    private static final int MAX_STATEMENT = 2000;

    private final QuizService quizService;
    private final CourseService courseService;
    private final QuizGenerator quizGenerator;
    private final TransactionTemplate readOnly;

    public QuestionSuggestionService(QuizService quizService, CourseService courseService,
                                     QuizGenerator quizGenerator, PlatformTransactionManager transactionManager) {
        this.quizService = quizService;
        this.courseService = courseService;
        this.quizGenerator = quizGenerator;
        this.readOnly = new TransactionTemplate(transactionManager);
        this.readOnly.setReadOnly(true);
    }

    /**
     * @param count nombre de questions demandé (borné à 1..{@value #MAX_COUNT})
     * @return suggestions valides, numérotées à la suite des questions existantes du quiz
     */
    public List<QuestionRequest> suggest(Long quizId, int count) {
        int wanted = Math.min(Math.max(count, 1), MAX_COUNT);
        // Lecture en base dans une transaction courte ; l'appel au modèle (plusieurs secondes) se fait hors transaction
        Source source = readOnly.execute(status -> {
            Quiz quiz = quizService.requireOwned(quizId);
            int lastPosition = quiz.getQuestions().stream().mapToInt(Question::getPosition).max().orElse(0);
            Long chapterId = quiz.getType() == QuizType.CONTROL ? quiz.getChapterId() : null;
            return new Source(courseService.aiContext(quiz.getCourseId(), chapterId, MAX_CONTEXT_CHARS), lastPosition);
        });

        GeneratedQuiz generated = quizGenerator.generate(source.content(), wanted);
        List<QuestionRequest> suggestions = new ArrayList<>();
        if (generated == null || generated.questions() == null) {
            return suggestions;
        }
        for (GeneratedQuiz.GeneratedQuestion question : generated.questions()) {
            if (suggestions.size() == wanted) {
                break;
            }
            QuestionRequest request = toRequest(question, source.lastPosition() + suggestions.size() + 1);
            if (request != null) {
                suggestions.add(request);
            }
        }
        return suggestions;
    }

    /** Convertit une question générée, ou renvoie {@code null} si elle ne respecte pas les règles d'une question. */
    static QuestionRequest toRequest(GeneratedQuiz.GeneratedQuestion question, int position) {
        if (question == null || question.type() == null || !StringUtils.hasText(question.statement())
                || question.options() == null) {
            return null;
        }
        List<OptionRequest> options = new ArrayList<>();
        for (GeneratedQuiz.GeneratedOption option : question.options()) {
            if (option != null && StringUtils.hasText(option.label())) {
                options.add(new OptionRequest(option.label().trim(), option.correct(), options.size() + 1));
            }
        }
        long correct = options.stream().filter(OptionRequest::correct).count();
        QuestionType type = QuestionType.valueOf(question.type().name());
        boolean valid = options.size() >= 2 && options.size() <= MAX_OPTIONS && correct >= 1
                && question.statement().length() <= MAX_STATEMENT
                && switch (type) {
                    case SINGLE_CHOICE -> correct == 1;
                    case TRUE_FALSE -> options.size() == 2 && correct == 1;
                    case MULTIPLE_CHOICE -> true;
                };
        return valid ? new QuestionRequest(question.statement().trim(), type, 1, position, options) : null;
    }

    private record Source(String content, int lastPosition) {
    }
}
