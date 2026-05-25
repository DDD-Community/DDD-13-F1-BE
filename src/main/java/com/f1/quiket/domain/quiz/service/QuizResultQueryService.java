package com.f1.quiket.domain.quiz.service;

import com.f1.quiket.domain.quiz.dto.QuizResultResponse;
import com.f1.quiket.domain.quiz.entity.Question;
import com.f1.quiket.domain.quiz.entity.QuestionAnswer;
import com.f1.quiket.domain.quiz.entity.QuestionOption;
import com.f1.quiket.domain.quiz.entity.QuizPlayAnswer;
import com.f1.quiket.domain.quiz.entity.QuizPlaySession;
import com.f1.quiket.domain.quiz.entity.QuizResult;
import com.f1.quiket.domain.quiz.entity.QuizSession;
import com.f1.quiket.domain.quiz.repository.QuestionAnswerRepository;
import com.f1.quiket.domain.quiz.repository.QuestionOptionRepository;
import com.f1.quiket.domain.quiz.repository.QuestionRepository;
import com.f1.quiket.domain.quiz.repository.QuizPlayAnswerRepository;
import com.f1.quiket.domain.quiz.repository.QuizPlaySessionRepository;
import com.f1.quiket.domain.quiz.repository.QuizResultRepository;
import com.f1.quiket.domain.quiz.repository.QuizSessionRepository;
import com.f1.quiket.domain.subject.entity.Subject;
import com.f1.quiket.domain.subject.repository.SubjectRepository;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.domain.user.repository.UserRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizResultQueryService {

    private final QuizResultRepository quizResultRepository;
    private final QuizPlaySessionRepository quizPlaySessionRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final QuestionAnswerRepository questionAnswerRepository;
    private final QuizPlayAnswerRepository quizPlayAnswerRepository;
    private final QuizResultResponseAssembler quizResultResponseAssembler;

    public QuizResultResponse getQuizResult(Long userId, String resultPublicId) {
        User user = findUser(userId);
        QuizResult result = quizResultRepository.findByPublicIdAndUserId(resultPublicId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_RESULT_NOT_FOUND));
        QuizPlaySession playSession = quizPlaySessionRepository.findByIdAndUserId(result.getPlaySessionId(), userId)
                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_PLAY_SESSION_NOT_FOUND));
        QuizSession quizSession = quizSessionRepository.findByIdAndUserIdAndDeletedAtIsNull(result.getQuizSessionId(), userId)
                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_SESSION_NOT_FOUND));
        Subject subject = subjectRepository.findByIdAndUserIdAndDeletedAtIsNull(result.getSubjectId(), userId)
                .orElseThrow(() -> new CustomException(ErrorCode.SUBJECT_NOT_FOUND));

        List<Question> questions = findQuestions(userId, quizSession.getId());
        List<Long> questionIds = questions.stream()
                .map(Question::getId)
                .toList();
        Map<Long, List<QuestionOption>> optionsByQuestionId = getOptionsByQuestionId(questionIds);
        Map<Long, QuestionAnswer> answersByQuestionId = getAnswersByQuestionId(questionIds);
        List<QuizPlayAnswer> playAnswers = quizPlayAnswerRepository.findAllByPlaySessionId(playSession.getId());

        return quizResultResponseAssembler.build(
                result,
                playSession,
                quizSession,
                subject,
                user,
                questions,
                optionsByQuestionId,
                answersByQuestionId,
                playAnswers
        );
    }

    private User findUser(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_USER_NOT_FOUND));
    }

    private List<Question> findQuestions(Long userId, Long quizSessionId) {
        List<Question> questions = questionRepository.findAllByQuizSessionIdAndUserIdOrderByDisplayOrderAscIdAsc(
                quizSessionId,
                userId
        );
        if (questions.isEmpty()) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "완료된 퀴즈 세션에 문항이 존재하지 않습니다.");
        }
        return questions;
    }

    private Map<Long, List<QuestionOption>> getOptionsByQuestionId(List<Long> questionIds) {
        return questionOptionRepository.findAllByQuestionIdInOrderByQuestionIdAscOptionNumberAsc(questionIds)
                .stream()
                .collect(Collectors.groupingBy(QuestionOption::getQuestionId));
    }

    private Map<Long, QuestionAnswer> getAnswersByQuestionId(List<Long> questionIds) {
        Map<Long, QuestionAnswer> answersByQuestionId = questionAnswerRepository.findAllByQuestionIdIn(questionIds)
                .stream()
                .collect(Collectors.toMap(QuestionAnswer::getQuestionId, Function.identity()));
        if (answersByQuestionId.size() != questionIds.size()) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "퀴즈 문항 정답 정보가 올바르지 않습니다.");
        }
        return answersByQuestionId;
    }
}
