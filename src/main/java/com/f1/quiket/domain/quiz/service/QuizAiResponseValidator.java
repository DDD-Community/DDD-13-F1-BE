package com.f1.quiket.domain.quiz.service;

import com.f1.quiket.domain.quiz.dto.QuizAiGeneratedOption;
import com.f1.quiket.domain.quiz.dto.QuizAiGeneratedQuestion;
import com.f1.quiket.domain.quiz.dto.QuizAiGenerationRequest;
import com.f1.quiket.domain.quiz.dto.QuizAiGenerationResponse;
import com.f1.quiket.domain.quiz.exception.QuizAiResponseValidationException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class QuizAiResponseValidator {

    private static final String QUIZ_TYPE_MULTIPLE_CHOICE = "multiple_choice";
    private static final String QUIZ_TYPE_OX = "ox";
    private static final int MIN_SIMILARITY_WORD_COUNT = 3;
    private static final double HISTORICAL_SIMILARITY_THRESHOLD = 0.8;
    private static final Set<String> DIFFICULTIES = Set.of("easy", "medium", "hard");
    private static final Set<String> GENERIC_OPTION_CONTENTS = Set.of(
            "모두맞다",
            "모두옳다",
            "모두정답이다",
            "위의모든것",
            "정답없음",
            "해당없음",
            "알수없음"
    );
    private static final Set<String> QUESTION_STOP_WORDS = Set.of(
            "다음",
            "보기",
            "중",
            "가장",
            "옳은",
            "옳지",
            "않은",
            "것",
            "것은",
            "무엇",
            "무엇인가",
            "어느",
            "해당",
            "설명",
            "대한",
            "이유",
            "이유는",
            "목적",
            "목적은",
            "경우",
            "맞는",
            "틀린",
            "위해"
    );
    private static final List<String> WORD_SUFFIXES = List.of(
            "에서는",
            "으로는",
            "이라는",
            "에서",
            "으로",
            "에게",
            "부터",
            "까지",
            "처럼",
            "보다",
            "하기",
            "하는",
            "된다",
            "되다",
            "하다",
            "이며",
            "이다",
            "해서",
            "하고",
            "하며",
            "거나",
            "라도",
            "지만",
            "는데",
            "은",
            "는",
            "이",
            "가",
            "을",
            "를",
            "의",
            "에",
            "도",
            "와",
            "과",
            "로",
            "기",
            "다"
    );

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

        Set<String> questionBodies = new HashSet<>();
        for (QuizAiGeneratedQuestion question : response.getQuestions()) {
            validateQuestion(request, question, allowedPartIds);
            if (!questionBodies.add(normalizeForComparison(question.getBody()))) {
                throw invalidResponse("동일한 문항 본문이 중복되었습니다.");
            }
            validateHistoricalDuplication(request, question.getBody());
        }
    }

    private void validateHistoricalDuplication(QuizAiGenerationRequest request, String questionBody) {
        if (request.excludedQuestionBodies() == null || request.excludedQuestionBodies().isEmpty()) {
            return;
        }

        String normalizedQuestion = normalizeForComparison(questionBody);
        Set<String> questionWords = contentWords(questionBody);
        for (String excludedQuestion : request.excludedQuestionBodies()) {
            if (!StringUtils.hasText(excludedQuestion)) {
                continue;
            }
            if (normalizedQuestion.equals(normalizeForComparison(excludedQuestion))) {
                throw invalidResponse("과거에 출제된 문항과 중복됩니다.");
            }
            if (isHighlySimilar(questionWords, contentWords(excludedQuestion))) {
                throw invalidResponse("과거에 출제된 문항과 지나치게 유사합니다.");
            }
        }
    }

    private boolean isHighlySimilar(Set<String> firstWords, Set<String> secondWords) {
        if (firstWords.size() < MIN_SIMILARITY_WORD_COUNT || secondWords.size() < MIN_SIMILARITY_WORD_COUNT) {
            return false;
        }

        Set<String> intersection = new HashSet<>(firstWords);
        intersection.retainAll(secondWords);
        Set<String> union = new HashSet<>(firstWords);
        union.addAll(secondWords);
        return (double) intersection.size() / union.size() >= HISTORICAL_SIMILARITY_THRESHOLD;
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
        if (!request.difficulty().equals(question.getDifficulty())) {
            throw invalidResponse("요청과 다른 난이도의 문항이 포함되었습니다.");
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
        Set<String> optionContents = new HashSet<>();
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

            String normalizedContent = normalizeForComparison(option.getContent());
            if (!optionContents.add(normalizedContent)) {
                throw invalidResponse("동일한 객관식 선택지가 중복되었습니다.");
            }
            if (GENERIC_OPTION_CONTENTS.contains(normalizedContent)) {
                throw invalidResponse("포괄적이거나 무의미한 객관식 선택지가 포함되었습니다.");
            }
            if (isCircularOption(question.getBody(), option.getContent())) {
                throw invalidResponse("질문 표현을 반복하는 순환형 객관식 선택지가 포함되었습니다.");
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

    private boolean isCircularOption(String questionBody, String optionContent) {
        Set<String> questionWords = contentWords(questionBody);
        Set<String> optionWords = contentWords(optionContent);
        return optionWords.size() >= 2 && questionWords.containsAll(optionWords);
    }

    private Set<String> contentWords(String value) {
        return Arrays.stream(value.toLowerCase(Locale.ROOT).split("[^가-힣a-z0-9]+"))
                .map(this::canonicalizeWord)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    private String canonicalizeWord(String word) {
        if (QUESTION_STOP_WORDS.contains(word)) {
            return "";
        }
        for (String suffix : WORD_SUFFIXES) {
            if (word.endsWith(suffix) && word.length() - suffix.length() >= 2) {
                String stem = word.substring(0, word.length() - suffix.length());
                return QUESTION_STOP_WORDS.contains(stem) ? "" : stem;
            }
        }
        return word.length() >= 2 ? word : "";
    }

    private String normalizeForComparison(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^가-힣a-z0-9]", "");
    }

    private QuizAiResponseValidationException invalidResponse(String message) {
        return new QuizAiResponseValidationException(message);
    }
}
