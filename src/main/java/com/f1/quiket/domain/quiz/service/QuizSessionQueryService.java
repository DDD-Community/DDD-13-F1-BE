package com.f1.quiket.domain.quiz.service;

import com.f1.quiket.domain.chapter.entity.Chapter;
import com.f1.quiket.domain.chapter.repository.ChapterRepository;
import com.f1.quiket.domain.part.entity.Part;
import com.f1.quiket.domain.part.repository.PartRepository;
import com.f1.quiket.domain.quiz.dto.QuizSessionResponse;
import com.f1.quiket.domain.quiz.entity.Question;
import com.f1.quiket.domain.quiz.entity.QuestionAnswer;
import com.f1.quiket.domain.quiz.entity.QuestionOption;
import com.f1.quiket.domain.quiz.entity.QuizSession;
import com.f1.quiket.domain.quiz.repository.QuestionAnswerRepository;
import com.f1.quiket.domain.quiz.repository.QuestionOptionRepository;
import com.f1.quiket.domain.quiz.repository.QuestionRepository;
import com.f1.quiket.domain.quiz.repository.QuizSessionRepository;
import com.f1.quiket.domain.subject.entity.Subject;
import com.f1.quiket.domain.subject.repository.SubjectRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 퀴즈 세트 조회 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizSessionQueryService {

    private static final String STATUS_COMPLETED = "completed";

    private final QuizSessionRepository quizSessionRepository;
    private final SubjectRepository subjectRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final QuestionAnswerRepository questionAnswerRepository;
    private final ChapterRepository chapterRepository;
    private final PartRepository partRepository;

    /**
     * 퀴즈 세트 조회
     */
    public QuizSessionResponse getQuizSession(Long userId, String quizSessionPublicId) {
        QuizSession quizSession = quizSessionRepository
                .findByPublicIdAndUserIdAndDeletedAtIsNull(quizSessionPublicId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_SESSION_NOT_FOUND));
        validateCompleted(quizSession);

        Subject subject = subjectRepository
                .findByIdAndUserIdAndDeletedAtIsNull(quizSession.getSubjectId(), userId)
                .orElseThrow(() -> new CustomException(ErrorCode.SUBJECT_NOT_FOUND));
        List<Question> questions = questionRepository.findAllByQuizSessionIdAndUserIdOrderByDisplayOrderAscIdAsc(
                quizSession.getId(),
                userId
        );
        validateQuestionsNotEmpty(questions);
        List<Long> questionIds = questions.stream()
                .map(Question::getId)
                .toList();
        Map<Long, List<QuestionOption>> optionsByQuestionId = getOptionsByQuestionId(questionIds);
        Map<Long, QuestionAnswer> answersByQuestionId = getAnswersByQuestionId(questionIds);
        Map<Long, Chapter> chaptersById = getChaptersById(userId, questions);
        Map<Long, Part> partsById = getPartsById(userId, questions);
        validateQuestionDetails(questions, answersByQuestionId, chaptersById, partsById);

        return QuizSessionResponse.of(
                quizSession,
                subject,
                questions,
                optionsByQuestionId,
                answersByQuestionId,
                chaptersById,
                partsById
        );
    }

    private void validateCompleted(QuizSession quizSession) {
        if (!STATUS_COMPLETED.equals(quizSession.getStatus())) {
            throw new CustomException(ErrorCode.QUIZ_SESSION_NOT_COMPLETED);
        }
    }

    private void validateQuestionsNotEmpty(List<Question> questions) {
        if (questions.isEmpty()) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "완료된 퀴즈 세션에 문항이 존재하지 않습니다.");
        }
    }

    private Map<Long, List<QuestionOption>> getOptionsByQuestionId(List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return Map.of();
        }

        return questionOptionRepository.findAllByQuestionIdInOrderByQuestionIdAscOptionNumberAsc(questionIds)
                .stream()
                .collect(Collectors.groupingBy(QuestionOption::getQuestionId));
    }

    private Map<Long, QuestionAnswer> getAnswersByQuestionId(List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return Map.of();
        }

        return questionAnswerRepository.findAllByQuestionIdIn(questionIds)
                .stream()
                .collect(Collectors.toMap(QuestionAnswer::getQuestionId, Function.identity()));
    }

    private Map<Long, Chapter> getChaptersById(Long userId, List<Question> questions) {
        List<Long> chapterIds = questions.stream()
                .map(Question::getChapterId)
                .distinct()
                .toList();
        if (chapterIds.isEmpty()) {
            return Map.of();
        }

        return chapterRepository.findAllByIdInAndUserIdAndDeletedAtIsNull(chapterIds, userId)
                .stream()
                .collect(Collectors.toMap(Chapter::getId, Function.identity()));
    }

    private Map<Long, Part> getPartsById(Long userId, List<Question> questions) {
        List<Long> partIds = questions.stream()
                .map(Question::getPartId)
                .distinct()
                .toList();
        if (partIds.isEmpty()) {
            return Map.of();
        }

        return partRepository.findAllByIdInAndUserIdAndDeletedAtIsNull(partIds, userId)
                .stream()
                .collect(Collectors.toMap(Part::getId, Function.identity()));
    }

    private void validateQuestionDetails(
            List<Question> questions,
            Map<Long, QuestionAnswer> answersByQuestionId,
            Map<Long, Chapter> chaptersById,
            Map<Long, Part> partsById
    ) {
        boolean invalid = questions.stream()
                .anyMatch(question -> !answersByQuestionId.containsKey(question.getId())
                        || !chaptersById.containsKey(question.getChapterId())
                        || !partsById.containsKey(question.getPartId()));
        if (invalid) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "퀴즈 문항 정보가 올바르지 않습니다.");
        }
    }
}
