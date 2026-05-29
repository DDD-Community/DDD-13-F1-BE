package com.f1.quiket.domain.home.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.chapter.entity.Chapter;
import com.f1.quiket.domain.chapter.repository.ChapterRepository;
import com.f1.quiket.domain.home.dto.HomeDataResponse;
import com.f1.quiket.domain.home.dto.RecentActivityPageResponse;
import com.f1.quiket.domain.home.dto.RecentActivityType;
import com.f1.quiket.domain.home.repository.SubjectCountProjection;
import com.f1.quiket.domain.home.repository.SubjectLastActivityProjection;
import com.f1.quiket.domain.part.entity.Part;
import com.f1.quiket.domain.part.repository.PartRepository;
import com.f1.quiket.domain.quiz.entity.QuizPlaySession;
import com.f1.quiket.domain.quiz.entity.QuizResult;
import com.f1.quiket.domain.quiz.entity.QuizSession;
import com.f1.quiket.domain.quiz.repository.QuizPlaySessionRepository;
import com.f1.quiket.domain.quiz.repository.QuizResultRepository;
import com.f1.quiket.domain.quiz.repository.QuizSessionRepository;
import com.f1.quiket.domain.subject.entity.Subject;
import com.f1.quiket.domain.subject.entity.SubjectExamSchedule;
import com.f1.quiket.domain.subject.repository.SubjectExamScheduleRepository;
import com.f1.quiket.domain.subject.repository.SubjectRepository;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class HomeServiceTest {

    private UserRepository userRepository;
    private SubjectRepository subjectRepository;
    private SubjectExamScheduleRepository subjectExamScheduleRepository;
    private ChapterRepository chapterRepository;
    private PartRepository partRepository;
    private QuizSessionRepository quizSessionRepository;
    private QuizPlaySessionRepository quizPlaySessionRepository;
    private QuizResultRepository quizResultRepository;
    private HomeService homeService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        subjectRepository = mock(SubjectRepository.class);
        subjectExamScheduleRepository = mock(SubjectExamScheduleRepository.class);
        chapterRepository = mock(ChapterRepository.class);
        partRepository = mock(PartRepository.class);
        quizSessionRepository = mock(QuizSessionRepository.class);
        quizPlaySessionRepository = mock(QuizPlaySessionRepository.class);
        quizResultRepository = mock(QuizResultRepository.class);

        homeService = new HomeService(
                userRepository,
                subjectRepository,
                subjectExamScheduleRepository,
                chapterRepository,
                partRepository,
                quizSessionRepository,
                quizPlaySessionRepository,
                quizResultRepository
        );
    }

    @Test
    void getHome_finds_active_quiz_from_all_activities_even_when_not_in_recent_five() {
        User user = user();
        Subject subject = subject(10L, "subject-public-id", "데이터베이스", LocalDateTime.now().minusDays(10));
        QuizSession inProgressQuiz = quizSession(1L, "quiz-in-progress", subject.getId(), "completed", 10, LocalDateTime.now().minusDays(3));
        QuizPlaySession inProgressPlay = playSession(101L, "play-in-progress", inProgressQuiz.getId(), subject.getId(), "in_progress", 3, LocalDateTime.now().minusDays(3));
        List<QuizSession> completedQuizzes = java.util.stream.IntStream.rangeClosed(2, 6)
                .mapToObj(index -> quizSession((long) index, "quiz-completed-" + index, subject.getId(), "completed", 10, LocalDateTime.now().minusHours(index)))
                .toList();
        List<QuizPlaySession> submittedPlays = completedQuizzes.stream()
                .map(quiz -> playSession(quiz.getId() + 200, "play-completed-" + quiz.getId(), quiz.getId(), subject.getId(), "submitted", 10, quiz.getCreatedAt().plusMinutes(10)))
                .toList();
        List<QuizResult> results = submittedPlays.stream()
                .map(play -> quizResult(play.getId() + 300, "result-public-" + play.getId(), play.getId(), play.getQuizSessionId(), subject.getId(), play.getUpdatedAt().plusMinutes(10)))
                .toList();

        mockCommonHomeData(user, List.of(subject), List.of(), List.of(), List.of(), concat(List.of(inProgressQuiz), completedQuizzes), concat(List.of(inProgressPlay), submittedPlays), results);

        HomeDataResponse response = homeService.getHome(user.getPublicId());

        assertThat(response.getRecentActivities()).hasSize(5);
        assertThat(response.getRecentActivities())
                .allSatisfy(activity -> assertThat(activity.getActivityType()).isEqualTo(RecentActivityType.QUIZ_COMPLETED));
        assertThat(response.getHero().isHasActiveQuiz()).isTrue();
        assertThat(response.getHero().getActiveQuiz().getActivityType()).isEqualTo(RecentActivityType.QUIZ_IN_PROGRESS);
        assertThat(response.getHero().getActiveQuiz().getPlaySessionId()).isEqualTo("play-in-progress");
    }

    @Test
    void getRecentActivities_returns_enum_types_and_paged_content() {
        User user = user();
        Subject subject = subject(10L, "subject-public-id", "데이터베이스", LocalDateTime.now().minusDays(10));
        QuizSession readyQuiz = quizSession(1L, "quiz-ready", subject.getId(), "completed", 10, LocalDateTime.now().minusHours(3));
        QuizSession inProgressQuiz = quizSession(2L, "quiz-in-progress", subject.getId(), "completed", 10, LocalDateTime.now().minusHours(2));
        QuizSession completedQuiz = quizSession(3L, "quiz-completed", subject.getId(), "completed", 10, LocalDateTime.now().minusHours(1));
        QuizPlaySession inProgressPlay = playSession(201L, "play-in-progress", inProgressQuiz.getId(), subject.getId(), "in_progress", 4, LocalDateTime.now().minusHours(2));
        QuizPlaySession submittedPlay = playSession(202L, "play-completed", completedQuiz.getId(), subject.getId(), "submitted", 10, LocalDateTime.now().minusHours(1));
        QuizResult result = quizResult(301L, "result-public-id", submittedPlay.getId(), completedQuiz.getId(), subject.getId(), LocalDateTime.now());

        mockCommonHomeData(user, List.of(subject), List.of(), List.of(), List.of(), List.of(readyQuiz, inProgressQuiz, completedQuiz), List.of(inProgressPlay, submittedPlay), List.of(result));

        RecentActivityPageResponse response = homeService.getRecentActivities(user.getPublicId(), 0, 2);

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent().get(0).getActivityType()).isEqualTo(RecentActivityType.QUIZ_COMPLETED);
        assertThat(response.getContent().get(0).getResultId()).isEqualTo("result-public-id");
        assertThat(response.getContent().get(1).getActivityType()).isEqualTo(RecentActivityType.QUIZ_IN_PROGRESS);
        assertThat(response.getTotalElements()).isEqualTo(3);
        assertThat(response.getTotalPages()).isEqualTo(2);
        assertThat(response.isHasNext()).isTrue();
    }

    @Test
    void getHome_returns_one_future_dDay_card_with_schedule_public_id() {
        User user = user();
        Subject subject = subject(10L, "subject-public-id", "데이터베이스", LocalDateTime.now().minusDays(10));
        SubjectExamSchedule schedule = schedule(100L, "schedule-public-id", subject.getId(), user.getId(), "중간고사", LocalDate.now().plusDays(3));

        mockCommonHomeData(user, List.of(subject), List.of(chapter(1L, subject.getId(), user.getId())), List.of(part(1L, subject.getId(), user.getId())), List.of(schedule), List.of(), List.of(), List.of());
        when(subjectExamScheduleRepository.findByUserIdAndDeletedAtIsNullAndExamDateGreaterThanEqualOrderByExamDateAscCreatedAtDesc(eq(user.getId()), any(LocalDate.class), any()))
                .thenReturn(List.of(schedule));

        HomeDataResponse response = homeService.getHome(user.getPublicId());

        assertThat(response.getDDayCards()).hasSize(1);
        assertThat(response.getDDayCards().get(0).getId()).isEqualTo("schedule-public-id");
        assertThat(response.getSubjects().get(0).getChapterCount()).isEqualTo(1);
        assertThat(response.getSubjects().get(0).getPartCount()).isEqualTo(1);
        assertThat(response.getSubjects().get(0).getExamSchedule().getId()).isEqualTo("schedule-public-id");
    }

    private void mockCommonHomeData(User user, List<Subject> subjects, List<Chapter> chapters, List<Part> parts, List<SubjectExamSchedule> schedules, List<QuizSession> quizSessions, List<QuizPlaySession> playSessions, List<QuizResult> quizResults) {
        when(userRepository.findByPublicIdAndDeletedAtIsNull(user.getPublicId())).thenReturn(Optional.of(user));
        when(subjectRepository.findHomeSummarySubjects(eq(user.getId()), anyInt())).thenReturn(subjects);
        when(subjectRepository.findAllByIdInAndUserIdAndDeletedAtIsNull(any(), eq(user.getId()))).thenReturn(subjects);
        when(chapterRepository.countBySubjectIds(eq(user.getId()), any())).thenReturn(countProjections(chapters, Chapter::getSubjectId));
        when(partRepository.countBySubjectIds(eq(user.getId()), any())).thenReturn(countProjections(parts, Part::getSubjectId));
        when(subjectExamScheduleRepository.findAllBySubjectIdInAndDeletedAtIsNull(any())).thenReturn(schedules);
        when(subjectExamScheduleRepository.findByUserIdAndDeletedAtIsNullAndExamDateGreaterThanEqualOrderByExamDateAscCreatedAtDesc(eq(user.getId()), any(LocalDate.class), any()))
                .thenReturn(List.of());
        when(quizSessionRepository.findReadyActivities(eq(user.getId()), eq("completed"), any()))
                .thenReturn(readyQuizSessions(quizSessions, playSessions));
        when(quizSessionRepository.countReadyActivities(user.getId(), "completed"))
                .thenReturn((long) readyQuizSessions(quizSessions, playSessions).size());
        when(quizSessionRepository.findAllByIdInAndUserIdAndDeletedAtIsNull(any(), eq(user.getId()))).thenReturn(quizSessions);
        when(quizSessionRepository.findLastActivityBySubjectIds(eq(user.getId()), any()))
                .thenReturn(lastActivityProjections(quizSessions, QuizSession::getSubjectId, QuizSession::getCreatedAt));
        when(quizPlaySessionRepository.findByUserIdAndStatusAndDeletedAtIsNullOrderByUpdatedAtDesc(eq(user.getId()), eq("in_progress"), any()))
                .thenReturn(playSessions.stream()
                        .filter(playSession -> "in_progress".equals(playSession.getStatus()))
                        .sorted(Comparator.comparing(QuizPlaySession::getUpdatedAt).reversed())
                        .toList());
        when(quizPlaySessionRepository.countByUserIdAndStatusAndDeletedAtIsNull(user.getId(), "in_progress"))
                .thenReturn(playSessions.stream()
                        .filter(playSession -> "in_progress".equals(playSession.getStatus()))
                        .count());
        when(quizPlaySessionRepository.findAllByIdInAndUserIdAndDeletedAtIsNull(any(), eq(user.getId()))).thenReturn(playSessions);
        when(quizPlaySessionRepository.findLastActivityBySubjectIds(eq(user.getId()), any()))
                .thenReturn(lastActivityProjections(playSessions, QuizPlaySession::getSubjectId, QuizPlaySession::getUpdatedAt));
        when(quizResultRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(eq(user.getId()), any()))
                .thenReturn(quizResults.stream()
                        .sorted(Comparator.comparing(QuizResult::getCreatedAt).reversed())
                        .toList());
        when(quizResultRepository.countByUserIdAndDeletedAtIsNull(user.getId())).thenReturn((long) quizResults.size());
        when(quizResultRepository.findLastActivityBySubjectIds(eq(user.getId()), any()))
                .thenReturn(lastActivityProjections(quizResults, QuizResult::getSubjectId, QuizResult::getCreatedAt));
    }

    private List<QuizSession> readyQuizSessions(List<QuizSession> quizSessions, List<QuizPlaySession> playSessions) {
        List<Long> startedQuizSessionIds = playSessions.stream()
                .map(QuizPlaySession::getQuizSessionId)
                .toList();
        return quizSessions.stream()
                .filter(session -> "completed".equals(session.getStatus()))
                .filter(session -> !startedQuizSessionIds.contains(session.getId()))
                .sorted(Comparator.comparing(QuizSession::getCompletedAt).reversed())
                .toList();
    }

    private <T> List<SubjectCountProjection> countProjections(List<T> values, Function<T, Long> subjectIdExtractor) {
        return values.stream()
                .collect(Collectors.groupingBy(subjectIdExtractor, Collectors.counting()))
                .entrySet()
                .stream()
                .map(entry -> subjectCountProjection(entry.getKey(), entry.getValue()))
                .toList();
    }

    private SubjectCountProjection subjectCountProjection(Long subjectId, Long itemCount) {
        return new SubjectCountProjection() {
            @Override
            public Long getSubjectId() {
                return subjectId;
            }

            @Override
            public Long getItemCount() {
                return itemCount;
            }
        };
    }

    private <T> List<SubjectLastActivityProjection> lastActivityProjections(
            List<T> values,
            Function<T, Long> subjectIdExtractor,
            Function<T, LocalDateTime> lastActivityAtExtractor
    ) {
        return values.stream()
                .collect(Collectors.groupingBy(
                        subjectIdExtractor,
                        Collectors.mapping(lastActivityAtExtractor, Collectors.maxBy(LocalDateTime::compareTo))
                ))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().isPresent())
                .map(entry -> subjectLastActivityProjection(entry.getKey(), entry.getValue().get()))
                .toList();
    }

    private SubjectLastActivityProjection subjectLastActivityProjection(Long subjectId, LocalDateTime lastActivityAt) {
        return new SubjectLastActivityProjection() {
            @Override
            public Long getSubjectId() {
                return subjectId;
            }

            @Override
            public LocalDateTime getLastActivityAt() {
                return lastActivityAt;
            }
        };
    }

    private User user() {
        User user = User.create("user-public-id", "user@example.com", "도토리");
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(user, "dotoriBalance", 12);
        ReflectionTestUtils.setField(user, "xpTotal", 360);
        ReflectionTestUtils.setField(user, "currentLevel", 3);
        return user;
    }

    private Subject subject(Long id, String publicId, String name, LocalDateTime createdAt) {
        Subject subject = newEntity(Subject.class);
        ReflectionTestUtils.setField(subject, "id", id);
        ReflectionTestUtils.setField(subject, "publicId", publicId);
        ReflectionTestUtils.setField(subject, "userId", 1L);
        ReflectionTestUtils.setField(subject, "name", name);
        ReflectionTestUtils.setField(subject, "purpose", "exam");
        ReflectionTestUtils.setField(subject, "createdAt", createdAt);
        return subject;
    }

    private Chapter chapter(Long id, Long subjectId, Long userId) {
        Chapter chapter = newEntity(Chapter.class);
        ReflectionTestUtils.setField(chapter, "id", id);
        ReflectionTestUtils.setField(chapter, "subjectId", subjectId);
        ReflectionTestUtils.setField(chapter, "userId", userId);
        return chapter;
    }

    private Part part(Long id, Long subjectId, Long userId) {
        Part part = newEntity(Part.class);
        ReflectionTestUtils.setField(part, "id", id);
        ReflectionTestUtils.setField(part, "subjectId", subjectId);
        ReflectionTestUtils.setField(part, "userId", userId);
        return part;
    }

    private SubjectExamSchedule schedule(Long id, String publicId, Long subjectId, Long userId, String examName, LocalDate examDate) {
        SubjectExamSchedule schedule = newEntity(SubjectExamSchedule.class);
        ReflectionTestUtils.setField(schedule, "id", id);
        ReflectionTestUtils.setField(schedule, "publicId", publicId);
        ReflectionTestUtils.setField(schedule, "subjectId", subjectId);
        ReflectionTestUtils.setField(schedule, "userId", userId);
        ReflectionTestUtils.setField(schedule, "examName", examName);
        ReflectionTestUtils.setField(schedule, "examDate", examDate);
        return schedule;
    }

    private QuizSession quizSession(Long id, String publicId, Long subjectId, String status, Integer questionCount, LocalDateTime createdAt) {
        QuizSession quizSession = newEntity(QuizSession.class);
        ReflectionTestUtils.setField(quizSession, "id", id);
        ReflectionTestUtils.setField(quizSession, "publicId", publicId);
        ReflectionTestUtils.setField(quizSession, "userId", 1L);
        ReflectionTestUtils.setField(quizSession, "subjectId", subjectId);
        ReflectionTestUtils.setField(quizSession, "quizType", "multiple_choice");
        ReflectionTestUtils.setField(quizSession, "questionCount", questionCount);
        ReflectionTestUtils.setField(quizSession, "status", status);
        ReflectionTestUtils.setField(quizSession, "createdAt", createdAt);
        ReflectionTestUtils.setField(quizSession, "updatedAt", createdAt);
        ReflectionTestUtils.setField(quizSession, "completedAt", createdAt);
        return quizSession;
    }

    private QuizPlaySession playSession(Long id, String clientSessionId, Long quizSessionId, Long subjectId, String status, Integer lastQuestionIndex, LocalDateTime updatedAt) {
        QuizPlaySession playSession = newEntity(QuizPlaySession.class);
        ReflectionTestUtils.setField(playSession, "id", id);
        ReflectionTestUtils.setField(playSession, "clientSessionId", clientSessionId);
        ReflectionTestUtils.setField(playSession, "quizSessionId", quizSessionId);
        ReflectionTestUtils.setField(playSession, "userId", 1L);
        ReflectionTestUtils.setField(playSession, "subjectId", subjectId);
        ReflectionTestUtils.setField(playSession, "status", status);
        ReflectionTestUtils.setField(playSession, "lastQuestionIndex", lastQuestionIndex);
        ReflectionTestUtils.setField(playSession, "createdAt", updatedAt.minusMinutes(5));
        ReflectionTestUtils.setField(playSession, "updatedAt", updatedAt);
        return playSession;
    }

    private QuizResult quizResult(Long id, String publicId, Long playSessionId, Long quizSessionId, Long subjectId, LocalDateTime createdAt) {
        QuizResult result = newEntity(QuizResult.class);
        ReflectionTestUtils.setField(result, "id", id);
        ReflectionTestUtils.setField(result, "publicId", publicId);
        ReflectionTestUtils.setField(result, "playSessionId", playSessionId);
        ReflectionTestUtils.setField(result, "quizSessionId", quizSessionId);
        ReflectionTestUtils.setField(result, "userId", 1L);
        ReflectionTestUtils.setField(result, "subjectId", subjectId);
        ReflectionTestUtils.setField(result, "totalCount", 10);
        ReflectionTestUtils.setField(result, "correctCount", 8);
        ReflectionTestUtils.setField(result, "createdAt", createdAt);
        ReflectionTestUtils.setField(result, "updatedAt", createdAt);
        return result;
    }

    @SafeVarargs
    private <T> List<T> concat(List<T>... values) {
        return java.util.Arrays.stream(values)
                .flatMap(List::stream)
                .toList();
    }

    private <T> T newEntity(Class<T> type) {
        return org.springframework.beans.BeanUtils.instantiateClass(type);
    }
}
