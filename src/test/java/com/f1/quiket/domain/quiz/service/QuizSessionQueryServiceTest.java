package com.f1.quiket.domain.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class QuizSessionQueryServiceTest {

    private QuizSessionRepository quizSessionRepository;
    private SubjectRepository subjectRepository;
    private QuestionRepository questionRepository;
    private QuestionOptionRepository questionOptionRepository;
    private QuestionAnswerRepository questionAnswerRepository;
    private ChapterRepository chapterRepository;
    private PartRepository partRepository;
    private QuizSessionQueryService quizSessionQueryService;

    @BeforeEach
    void setUp() {
        quizSessionRepository = mock(QuizSessionRepository.class);
        subjectRepository = mock(SubjectRepository.class);
        questionRepository = mock(QuestionRepository.class);
        questionOptionRepository = mock(QuestionOptionRepository.class);
        questionAnswerRepository = mock(QuestionAnswerRepository.class);
        chapterRepository = mock(ChapterRepository.class);
        partRepository = mock(PartRepository.class);
        quizSessionQueryService = new QuizSessionQueryService(
                quizSessionRepository,
                subjectRepository,
                questionRepository,
                questionOptionRepository,
                questionAnswerRepository,
                chapterRepository,
                partRepository
        );
    }

    @Test
    void getQuizSession_returns_completed_quiz_set() {
        Long userId = 1L;
        Subject subject = subject(10L, "subject-public-id", userId, "데이터베이스");
        Chapter chapter = chapter(100L, "chapter-public-id", subject.getId(), userId, "1장 데이터 모델링", 1);
        Part part = part(1000L, "part-public-id", chapter.getId(), subject.getId(), userId, "데이터 모델링 개념");
        QuizSession quizSession = quizSession(500L, "quiz-session-public-id", userId, subject.getId(), "completed");
        Question question = question(
                900L,
                "question-public-id",
                quizSession.getId(),
                userId,
                subject.getId(),
                chapter.getId(),
                part.getId(),
                1
        );
        QuestionOption option1 = option(10000L, question.getId(), 1, "추상화", true);
        QuestionOption option2 = option(10001L, question.getId(), 2, "정규화", false);
        QuestionAnswer answer = answer(20000L, question.getId(), "1");

        when(quizSessionRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(quizSession.getPublicId(), userId))
                .thenReturn(Optional.of(quizSession));
        when(subjectRepository.findByIdAndUserIdAndDeletedAtIsNull(subject.getId(), userId))
                .thenReturn(Optional.of(subject));
        when(questionRepository.findAllByQuizSessionIdAndUserIdOrderByDisplayOrderAscIdAsc(quizSession.getId(), userId))
                .thenReturn(List.of(question));
        when(questionOptionRepository.findAllByQuestionIdInOrderByQuestionIdAscOptionNumberAsc(List.of(question.getId())))
                .thenReturn(List.of(option1, option2));
        when(questionAnswerRepository.findAllByQuestionIdIn(List.of(question.getId())))
                .thenReturn(List.of(answer));
        when(chapterRepository.findAllByIdInAndUserIdAndDeletedAtIsNull(List.of(chapter.getId()), userId))
                .thenReturn(List.of(chapter));
        when(partRepository.findAllByIdInAndUserIdAndDeletedAtIsNull(List.of(part.getId()), userId))
                .thenReturn(List.of(part));

        QuizSessionResponse response = quizSessionQueryService.getQuizSession(userId, quizSession.getPublicId());

        assertThat(response.getId()).isEqualTo(quizSession.getPublicId());
        assertThat(response.getSubjectId()).isEqualTo(subject.getPublicId());
        assertThat(response.getSubjectName()).isEqualTo("데이터베이스");
        assertThat(response.getQuizType()).isEqualTo("multiple_choice");
        assertThat(response.getChoiceCount()).isEqualTo(4);
        assertThat(response.getQuestionCount()).isEqualTo(1);
        assertThat(response.getPlayMode()).isEqualTo("one_by_one");
        assertThat(response.getTimerEnabled()).isTrue();
        assertThat(response.getTimerScope()).isEqualTo("per_question");
        assertThat(response.getTimerSeconds()).isEqualTo(60);
        assertThat(response.getDifficulty()).isEqualTo("medium");
        assertThat(response.getStatus()).isEqualTo("completed");
        assertThat(response.getQuestions()).hasSize(1);
        assertThat(response.getQuestions().get(0).getId()).isEqualTo(question.getPublicId());
        assertThat(response.getQuestions().get(0).getChapterId()).isEqualTo(chapter.getPublicId());
        assertThat(response.getQuestions().get(0).getPartId()).isEqualTo(part.getPublicId());
        assertThat(response.getQuestions().get(0).getPartName()).isEqualTo(part.getName());
        assertThat(response.getQuestions().get(0).getOptions()).hasSize(2);
        assertThat(response.getQuestions().get(0).getOptions().get(0).getId()).isEqualTo(String.valueOf(option1.getId()));
        assertThat(response.getQuestions().get(0).getOptions().get(0).getOptionNumber()).isEqualTo(1);
        assertThat(response.getQuestions().get(0).getAnswer().getAnswerValue()).isEqualTo("1");
    }

    @Test
    void getQuizSession_throws_not_completed_when_generation_pending() {
        Long userId = 1L;
        QuizSession quizSession = quizSession(500L, "quiz-session-public-id", userId, 10L, "pending");
        when(quizSessionRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(quizSession.getPublicId(), userId))
                .thenReturn(Optional.of(quizSession));

        assertThatThrownBy(() -> quizSessionQueryService.getQuizSession(userId, quizSession.getPublicId()))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.QUIZ_SESSION_NOT_COMPLETED);

        verifyNoInteractions(subjectRepository, questionRepository, questionOptionRepository, questionAnswerRepository);
    }

    @Test
    void getQuizSession_throws_not_found_when_quiz_session_missing() {
        Long userId = 1L;
        String quizSessionId = "quiz-session-public-id";
        when(quizSessionRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(quizSessionId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> quizSessionQueryService.getQuizSession(userId, quizSessionId))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.QUIZ_SESSION_NOT_FOUND);

        verifyNoInteractions(subjectRepository, questionRepository, questionOptionRepository, questionAnswerRepository);
    }

    @Test
    void getQuizSession_throws_internal_error_when_completed_session_has_no_questions() {
        Long userId = 1L;
        Subject subject = subject(10L, "subject-public-id", userId, "데이터베이스");
        QuizSession quizSession = quizSession(500L, "quiz-session-public-id", userId, subject.getId(), "completed");

        when(quizSessionRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(quizSession.getPublicId(), userId))
                .thenReturn(Optional.of(quizSession));
        when(subjectRepository.findByIdAndUserIdAndDeletedAtIsNull(subject.getId(), userId))
                .thenReturn(Optional.of(subject));
        when(questionRepository.findAllByQuizSessionIdAndUserIdOrderByDisplayOrderAscIdAsc(quizSession.getId(), userId))
                .thenReturn(List.of());

        assertThatThrownBy(() -> quizSessionQueryService.getQuizSession(userId, quizSession.getPublicId()))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);

        verifyNoInteractions(questionOptionRepository, questionAnswerRepository, chapterRepository, partRepository);
    }

    @Test
    void getQuizSession_throws_internal_error_when_answer_missing() {
        Long userId = 1L;
        Subject subject = subject(10L, "subject-public-id", userId, "데이터베이스");
        Chapter chapter = chapter(100L, "chapter-public-id", subject.getId(), userId, "1장 데이터 모델링", 1);
        Part part = part(1000L, "part-public-id", chapter.getId(), subject.getId(), userId, "데이터 모델링 개념");
        QuizSession quizSession = quizSession(500L, "quiz-session-public-id", userId, subject.getId(), "completed");
        Question question = question(
                900L,
                "question-public-id",
                quizSession.getId(),
                userId,
                subject.getId(),
                chapter.getId(),
                part.getId(),
                1
        );

        when(quizSessionRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(quizSession.getPublicId(), userId))
                .thenReturn(Optional.of(quizSession));
        when(subjectRepository.findByIdAndUserIdAndDeletedAtIsNull(subject.getId(), userId))
                .thenReturn(Optional.of(subject));
        when(questionRepository.findAllByQuizSessionIdAndUserIdOrderByDisplayOrderAscIdAsc(quizSession.getId(), userId))
                .thenReturn(List.of(question));
        when(questionOptionRepository.findAllByQuestionIdInOrderByQuestionIdAscOptionNumberAsc(List.of(question.getId())))
                .thenReturn(List.of());
        when(questionAnswerRepository.findAllByQuestionIdIn(List.of(question.getId())))
                .thenReturn(List.of());
        when(chapterRepository.findAllByIdInAndUserIdAndDeletedAtIsNull(List.of(chapter.getId()), userId))
                .thenReturn(List.of(chapter));
        when(partRepository.findAllByIdInAndUserIdAndDeletedAtIsNull(List.of(part.getId()), userId))
                .thenReturn(List.of(part));

        assertThatThrownBy(() -> quizSessionQueryService.getQuizSession(userId, quizSession.getPublicId()))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private QuizSession quizSession(Long id, String publicId, Long userId, Long subjectId, String status) {
        QuizSession quizSession = QuizSession.create(
                publicId,
                userId,
                subjectId,
                "multiple_choice",
                4,
                1,
                "one_by_one",
                true,
                "per_question",
                60,
                "medium",
                status,
                "quiz-job-1"
        );
        ReflectionTestUtils.setField(quizSession, "id", id);
        return quizSession;
    }

    private Question question(
            Long id,
            String publicId,
            Long quizSessionId,
            Long userId,
            Long subjectId,
            Long chapterId,
            Long partId,
            Integer displayOrder
    ) {
        Question question = newEntity(Question.class);
        ReflectionTestUtils.setField(question, "id", id);
        ReflectionTestUtils.setField(question, "publicId", publicId);
        ReflectionTestUtils.setField(question, "quizSessionId", quizSessionId);
        ReflectionTestUtils.setField(question, "userId", userId);
        ReflectionTestUtils.setField(question, "subjectId", subjectId);
        ReflectionTestUtils.setField(question, "chapterId", chapterId);
        ReflectionTestUtils.setField(question, "partId", partId);
        ReflectionTestUtils.setField(question, "questionType", "multiple_choice");
        ReflectionTestUtils.setField(question, "difficulty", "medium");
        ReflectionTestUtils.setField(question, "summary", "모델링의 핵심 특징");
        ReflectionTestUtils.setField(question, "body", "다음 중 데이터 모델링의 특징으로 올바른 것은?");
        ReflectionTestUtils.setField(question, "correctExplanation", "데이터 모델링은 현실 데이터를 추상화합니다.");
        ReflectionTestUtils.setField(question, "incorrectExplanation", "선택지는 데이터 모델링의 핵심 특징과 맞지 않습니다.");
        ReflectionTestUtils.setField(question, "displayOrder", displayOrder);
        return question;
    }

    private QuestionOption option(Long id, Long questionId, Integer optionNumber, String content, Boolean correct) {
        QuestionOption option = newEntity(QuestionOption.class);
        ReflectionTestUtils.setField(option, "id", id);
        ReflectionTestUtils.setField(option, "questionId", questionId);
        ReflectionTestUtils.setField(option, "optionNumber", optionNumber);
        ReflectionTestUtils.setField(option, "content", content);
        ReflectionTestUtils.setField(option, "correct", correct);
        return option;
    }

    private QuestionAnswer answer(Long id, Long questionId, String answerValue) {
        QuestionAnswer answer = newEntity(QuestionAnswer.class);
        ReflectionTestUtils.setField(answer, "id", id);
        ReflectionTestUtils.setField(answer, "questionId", questionId);
        ReflectionTestUtils.setField(answer, "answerValue", answerValue);
        return answer;
    }

    private Subject subject(Long id, String publicId, Long userId, String name) {
        Subject subject = newEntity(Subject.class);
        ReflectionTestUtils.setField(subject, "id", id);
        ReflectionTestUtils.setField(subject, "publicId", publicId);
        ReflectionTestUtils.setField(subject, "userId", userId);
        ReflectionTestUtils.setField(subject, "name", name);
        ReflectionTestUtils.setField(subject, "purpose", "exam");
        return subject;
    }

    private Chapter chapter(Long id, String publicId, Long subjectId, Long userId, String name, Integer displayOrder) {
        Chapter chapter = newEntity(Chapter.class);
        ReflectionTestUtils.setField(chapter, "id", id);
        ReflectionTestUtils.setField(chapter, "publicId", publicId);
        ReflectionTestUtils.setField(chapter, "subjectId", subjectId);
        ReflectionTestUtils.setField(chapter, "userId", userId);
        ReflectionTestUtils.setField(chapter, "name", name);
        ReflectionTestUtils.setField(chapter, "displayOrder", displayOrder);
        return chapter;
    }

    private Part part(Long id, String publicId, Long chapterId, Long subjectId, Long userId, String name) {
        Part part = newEntity(Part.class);
        ReflectionTestUtils.setField(part, "id", id);
        ReflectionTestUtils.setField(part, "publicId", publicId);
        ReflectionTestUtils.setField(part, "chapterId", chapterId);
        ReflectionTestUtils.setField(part, "subjectId", subjectId);
        ReflectionTestUtils.setField(part, "userId", userId);
        ReflectionTestUtils.setField(part, "name", name);
        ReflectionTestUtils.setField(part, "partNumber", 1);
        return part;
    }

    private <T> T newEntity(Class<T> type) {
        return org.springframework.beans.BeanUtils.instantiateClass(type);
    }
}
