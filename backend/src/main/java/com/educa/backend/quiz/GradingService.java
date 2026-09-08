package com.educa.backend.quiz;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.educa.backend.quiz.dto.AttemptSubmission.AnswerSubmission;

/** Correction automatique d'une tentative de quiz. Aucune dépendance de persistance. */
@Service
public class GradingService {

    public record GradeResult(BigDecimal score, int correctCount, int totalQuestions, List<AttemptAnswer> answers) {
    }

    public GradeResult grade(Quiz quiz, List<AnswerSubmission> submitted) {
        Map<Long, AnswerSubmission> byQuestion = submitted == null ? Map.of()
                : submitted.stream().collect(Collectors.toMap(AnswerSubmission::questionId, Function.identity(),
                        (a, b) -> a));

        int totalPoints = 0;
        int earnedPoints = 0;
        int correctCount = 0;
        List<AttemptAnswer> answers = new ArrayList<>();

        for (Question question : quiz.getQuestions()) {
            totalPoints += question.getPoints();

            Set<Long> optionIds = question.getOptions().stream().map(AnswerOption::getId).collect(Collectors.toSet());
            Set<Long> correctIds = question.getOptions().stream()
                    .filter(AnswerOption::isCorrect).map(AnswerOption::getId).collect(Collectors.toSet());

            AnswerSubmission answer = byQuestion.get(question.getId());
            Set<Long> selected = new HashSet<>();
            if (answer != null && answer.selectedOptionIds() != null) {
                selected.addAll(answer.selectedOptionIds());
            }
            selected.retainAll(optionIds); // ignore les ids d'options qui n'appartiennent pas à la question

            boolean ok = !correctIds.isEmpty() && selected.equals(correctIds);
            if (ok) {
                earnedPoints += question.getPoints();
                correctCount++;
            }

            AttemptAnswer snapshot = new AttemptAnswer();
            snapshot.setQuestionId(question.getId());
            snapshot.setSelectedOptionIds(selected.toArray(Long[]::new));
            snapshot.setCorrect(ok);
            answers.add(snapshot);
        }

        BigDecimal score = totalPoints == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(earnedPoints)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalPoints), 2, RoundingMode.HALF_UP);

        return new GradeResult(score, correctCount, quiz.getQuestions().size(), answers);
    }
}
