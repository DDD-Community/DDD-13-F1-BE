package com.f1.quiket.domain.gamification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.gamification.entity.UserDotoriLog;
import com.f1.quiket.domain.gamification.entity.UserStreakLog;
import com.f1.quiket.domain.gamification.entity.UserXpLog;
import com.f1.quiket.domain.gamification.repository.UserDotoriLogRepository;
import com.f1.quiket.domain.gamification.repository.UserStreakLogRepository;
import com.f1.quiket.domain.gamification.repository.UserXpLogRepository;
import com.f1.quiket.domain.quiz.entity.QuizPlaySession;
import com.f1.quiket.domain.quiz.entity.QuizSession;
import com.f1.quiket.domain.user.entity.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class GamificationRewardServiceTest {

    private UserXpLogRepository userXpLogRepository;
    private UserDotoriLogRepository userDotoriLogRepository;
    private UserStreakLogRepository userStreakLogRepository;
    private GamificationRewardService gamificationRewardService;

    @BeforeEach
    void setUp() {
        userXpLogRepository = mock(UserXpLogRepository.class);
        userDotoriLogRepository = mock(UserDotoriLogRepository.class);
        userStreakLogRepository = mock(UserStreakLogRepository.class);
        gamificationRewardService = new GamificationRewardService(
                userXpLogRepository,
                userDotoriLogRepository,
                userStreakLogRepository
        );
    }

    @Test
    void applyQuizReward_rewards_dotori_and_xp_for_first_play() {
        User user = user(1L, 10, 360, 3);
        QuizSession quizSession = quizSession(500L, user.getId(), 20L, "medium");
        QuizPlaySession playSession = playSession(700L, quizSession.getId(), user.getId(), quizSession.getSubjectId());
        LocalDate today = today();
        when(userStreakLogRepository.findByUserIdAndStudyDate(user.getId(), today))
                .thenReturn(Optional.empty());
        when(userStreakLogRepository.findTopByUserIdAndStudyDateBeforeOrderByStudyDateDesc(user.getId(), today))
                .thenReturn(Optional.empty());
        when(userStreakLogRepository.save(any(UserStreakLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        QuizRewardResult result = gamificationRewardService.applyQuizReward(user, playSession, quizSession, 2);

        assertThat(result.dotoriEarned()).isEqualTo(2);
        assertThat(result.xpEarned()).isEqualTo(8);
        assertThat(result.leveledUp()).isFalse();
        assertThat(result.currentDotoriBalance()).isEqualTo(12);
        assertThat(result.currentXpTotal()).isEqualTo(368);
        assertThat(user.getDotoriBalance()).isEqualTo(12);
        assertThat(user.getXpTotal()).isEqualTo(368);
        assertThat(user.getCurrentLevel()).isEqualTo(3);

        ArgumentCaptor<UserDotoriLog> dotoriCaptor = ArgumentCaptor.forClass(UserDotoriLog.class);
        verify(userDotoriLogRepository).save(dotoriCaptor.capture());
        assertThat(dotoriCaptor.getValue().getAmount()).isEqualTo(2);
        assertThat(dotoriCaptor.getValue().getBalanceBefore()).isEqualTo(10);
        assertThat(dotoriCaptor.getValue().getBalanceAfter()).isEqualTo(12);

        ArgumentCaptor<UserXpLog> xpCaptor = ArgumentCaptor.forClass(UserXpLog.class);
        verify(userXpLogRepository).save(xpCaptor.capture());
        assertThat(xpCaptor.getValue().getXpType()).isEqualTo("quiz_correct");
        assertThat(xpCaptor.getValue().getBaseXp()).isEqualTo(8);
        assertThat(xpCaptor.getValue().getStreakMultiplier()).isEqualByComparingTo(BigDecimal.valueOf(1.0));
        assertThat(xpCaptor.getValue().getEarnedXp()).isEqualTo(8);
        assertThat(xpCaptor.getValue().getXpBefore()).isEqualTo(360);
        assertThat(xpCaptor.getValue().getXpAfter()).isEqualTo(368);
    }

    @Test
    void applyQuizReward_updates_level_when_xp_crosses_threshold() {
        User user = user(1L, 0, 98, 1);
        QuizSession quizSession = quizSession(500L, user.getId(), 20L, "medium");
        QuizPlaySession playSession = playSession(700L, quizSession.getId(), user.getId(), quizSession.getSubjectId());
        UserStreakLog streak = UserStreakLog.create(user.getId(), today(), 1, BigDecimal.valueOf(1.0));
        when(userStreakLogRepository.findByUserIdAndStudyDate(user.getId(), today()))
                .thenReturn(Optional.of(streak));

        QuizRewardResult result = gamificationRewardService.applyQuizReward(user, playSession, quizSession, 1);

        assertThat(result.xpEarned()).isEqualTo(4);
        assertThat(result.leveledUp()).isTrue();
        assertThat(result.newLevel()).isEqualTo(2);
        assertThat(user.getXpTotal()).isEqualTo(102);
        assertThat(user.getCurrentLevel()).isEqualTo(2);
    }

    @Test
    void applyQuizReward_applies_consecutive_streak_multiplier() {
        User user = user(1L, 0, 0, 1);
        QuizSession quizSession = quizSession(500L, user.getId(), 20L, "medium");
        QuizPlaySession playSession = playSession(700L, quizSession.getId(), user.getId(), quizSession.getSubjectId());
        LocalDate today = today();
        UserStreakLog previous = UserStreakLog.create(user.getId(), today.minusDays(1), 2, BigDecimal.valueOf(1.1));
        when(userStreakLogRepository.findByUserIdAndStudyDate(user.getId(), today))
                .thenReturn(Optional.empty());
        when(userStreakLogRepository.findTopByUserIdAndStudyDateBeforeOrderByStudyDateDesc(user.getId(), today))
                .thenReturn(Optional.of(previous));
        when(userStreakLogRepository.save(any(UserStreakLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        QuizRewardResult result = gamificationRewardService.applyQuizReward(user, playSession, quizSession, 2);

        assertThat(result.xpEarned()).isEqualTo(9);

        ArgumentCaptor<UserStreakLog> streakCaptor = ArgumentCaptor.forClass(UserStreakLog.class);
        verify(userStreakLogRepository).save(streakCaptor.capture());
        assertThat(streakCaptor.getValue().getStreakCount()).isEqualTo(3);
        assertThat(streakCaptor.getValue().getMultiplier()).isEqualByComparingTo(BigDecimal.valueOf(1.2));
    }

    private LocalDate today() {
        return LocalDate.now(ZoneId.of("Asia/Seoul"));
    }

    private User user(Long id, Integer dotoriBalance, Integer xpTotal, Integer currentLevel) {
        User user = User.create("user-public-id", "user@example.com", "도토리");
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "dotoriBalance", dotoriBalance);
        ReflectionTestUtils.setField(user, "xpTotal", xpTotal);
        ReflectionTestUtils.setField(user, "currentLevel", currentLevel);
        return user;
    }

    private QuizSession quizSession(Long id, Long userId, Long subjectId, String difficulty) {
        QuizSession quizSession = QuizSession.create(
                "quiz-session-public-id",
                userId,
                subjectId,
                "multiple_choice",
                4,
                2,
                "one_by_one",
                true,
                "per_question",
                60,
                difficulty,
                "completed",
                "quiz-job-id"
        );
        ReflectionTestUtils.setField(quizSession, "id", id);
        return quizSession;
    }

    private QuizPlaySession playSession(Long id, Long quizSessionId, Long userId, Long subjectId) {
        QuizPlaySession playSession = QuizPlaySession.createFirst(
                "client-session-id",
                quizSessionId,
                userId,
                subjectId,
                false,
                true,
                null
        );
        ReflectionTestUtils.setField(playSession, "id", id);
        return playSession;
    }
}
