package com.f1.quiket.domain.quiz.service;

import com.f1.quiket.domain.quiz.dto.QuizAiGeneratedOption;
import com.f1.quiket.domain.quiz.dto.QuizAiGeneratedQuestion;
import com.f1.quiket.domain.quiz.dto.QuizAiGenerationRequest;
import com.f1.quiket.domain.quiz.dto.QuizAiGenerationResponse;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class QuizAiResponseValidator {

    private static final String QUIZ_TYPE_MULTIPLE_CHOICE = "multiple_choice";
    private static final String QUIZ_TYPE_OX = "ox";
    private static final Set<String> DIFFICULTIES = Set.of("easy", "medium", "hard");

    public void validate(QuizAiGenerationRequest request, QuizAiGenerationResponse response) {
        if (response == null || response.getQuestions() == null) {
            throw invalidResponse("AI 퀴즈 응답이 비어 있습니다.");
        }
        if (response.getQuestions().size() != request.questionCount()) {
            throw invalidResponse("AI 퀴즈 문항 수가 요청과 다릅니다.");
        }

        Set<String> allowedPartIds = request.parts().stream()
                .map(part -> part.getPublicId())
                .collect(Collectors.toSet());

        for (QuizAiGeneratedQuestion question : response.getQuestions()) {
            validateQuestion(request, question, allowedPartIds);
        }
    }

    private void validateQuestion(
            QuizAiGenerationRequest request,
            QuizAiGeneratedQuestion question,
            Set<String> allowedPartIds
    ) {
        if (question == null) {
            throw invalidResponse("AI 퀴즈 문항이 비어 있습니다.");
        }
        validateRequired(question.getPartId(), "partId");
        validateRequired(question.getQuestionType(), "questionType");
        validateRequired(question.getDifficulty(), "difficulty");
        validateRequired(question.getSummary(), "summary");
        validateRequired(question.getBody(), "body");
        validateRequired(question.getAnswerValue(), "answerValue");
        validateRequired(question.getCorrectExplanation(), "correctExplanation");
        validateRequired(question.getIncorrectExplanation(), "incorrectExplanation");

        if (!allowedPartIds.contains(question.getPartId())) {
            throw invalidResponse("출제 범위 밖 partId가 포함되었습니다.");
        }
        if (!request.quizType().equals(question.getQuestionType())) {
            throw invalidResponse("요청과 다른 문항 유형이 포함되었습니다.");
        }
        if (!DIFFICULTIES.contains(question.getDifficulty())) {
            throw invalidResponse("허용되지 않은 난이도가 포함되었습니다.");
        }
        if (question.getSummary().length() < 8 || question.getSummary().length() > 20) {
            throw invalidResponse("문항 요약 길이가 올바르지 않습니다.");
        }
        if (question.getCorrectExplanation().length() < 5 || question.getIncorrectExplanation().length() < 5) {
            throw invalidResponse("문항 해설이 너무 짧습니다.");
        }

        if (QUIZ_TYPE_MULTIPLE_CHOICE.equals(request.quizType())) {
            validateMultipleChoice(request, question);
            return;
        }
        if (QUIZ_TYPE_OX.equals(request.quizType())) {
            validateOx(question);
            return;
        }
        throw invalidResponse("지원하지 않는 문항 유형입니다.");
    }

    private void validateMultipleChoice(QuizAiGenerationRequest request, QuizAiGeneratedQuestion question) {
        List<QuizAiGeneratedOption> options = question.getOptions();
        if (options == null || options.size() != request.choiceCount()) {
            throw invalidResponse("객관식 보기 수가 요청과 다릅니다.");
        }

        Set<Integer> optionNumbers = new HashSet<>();
        for (QuizAiGeneratedOption option : options) {
            if (option == null || option.getOptionNumber() == null || !StringUtils.hasText(option.getContent())) {
                throw invalidResponse("객관식 선택지가 올바르지 않습니다.");
            }
            if (option.getOptionNumber() < 1 || option.getOptionNumber() > request.choiceCount()) {
                throw invalidResponse("객관식 선택지 번호가 범위를 벗어났습니다.");
            }
            if (!optionNumbers.add(option.getOptionNumber())) {
                throw invalidResponse("객관식 선택지 번호가 중복되었습니다.");
            }
        }

        try {
            int answerNumber = Integer.parseInt(question.getAnswerValue());
            if (!optionNumbers.contains(answerNumber)) {
                throw invalidResponse("객관식 정답 번호가 선택지에 없습니다.");
            }
        } catch (NumberFormatException e) {
            throw invalidResponse("객관식 정답 형식이 올바르지 않습니다.");
        }
    }

    private void validateOx(QuizAiGeneratedQuestion question) {
        if (question.getOptions() != null && !question.getOptions().isEmpty()) {
            throw invalidResponse("OX 문항에는 선택지가 없어야 합니다.");
        }
        if (!Set.of("O", "X").contains(question.getAnswerValue())) {
            throw invalidResponse("OX 정답 형식이 올바르지 않습니다.");
        }
    }

    private void validateRequired(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw invalidResponse("AI 퀴즈 필수 필드 누락: " + fieldName);
        }
    }

    private CustomException invalidResponse(String message) {
        return new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, message);
    }
}
