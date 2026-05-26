package com.f1.quiket.domain.gamification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
    void applyQuizReward_caps_xp_to_max_level_when_user_approaches_max() {
        // 명세 GAMIF-002 — 만렙 도달 후 추가 XP 누적 X. 진입선까지의 잔여 분만 적립.
        User user = user(1L, 0, GamificationLevelCalculator.MAX_LEVEL_MIN_XP - 2, 9);
        QuizSession quizSession = quizSession(500L, user.getId(), 20L, "hard");
        QuizPlaySession playSession = playSession(700L, quizSession.getId(), user.getId(), quizSession.getSubjectId());
        LocalDate today = today();
        when(userStreakLogRepository.findByUserIdAndStudyDate(user.getId(), today))
                .thenReturn(Optional.of(UserStreakLog.create(user.getId(), today, 1, BigDecimal.valueOf(1.0))));

        // first hard 정답 1개 → raw 5XP, 잔여 2XP만 적립되어야 함
        QuizRewardResult result = gamificationRewardService.applyQuizReward(user, playSession, quizSession, 1);

        assertThat(result.xpEarned()).isEqualTo(2);
        assertThat(user.getXpTotal()).isEqualTo(GamificationLevelCalculator.MAX_LEVEL_MIN_XP);
        assertThat(user.getCurrentLevel()).isEqualTo(10);
        assertThat(result.leveledUp()).isTrue();
        assertThat(result.newLevel()).isEqualTo(10);

        ArgumentCaptor<UserXpLog> xpCaptor = ArgumentCaptor.forClass(UserXpLog.class);
        verify(userXpLogRepository).save(xpCaptor.capture());
        // 로그 적립 값도 cap 후 값과 일치 (응답·로그·user.xpTotal 일관성)
        assertThat(xpCaptor.getValue().getEarnedXp()).isEqualTo(2);
        assertThat(xpCaptor.getValue().getXpAfter()).isEqualTo(GamificationLevelCalculator.MAX_LEVEL_MIN_XP);
    }

    @Test
    void applyQuizReward_blocks_xp_after_max_level_reached() {
        // 명세 GAMIF-002 — 만렙(Lv.10) 도달 후 추가 풀이는 XP 누적 X.
        User user = user(1L, 0, GamificationLevelCalculator.MAX_LEVEL_MIN_XP, 10);
        QuizSession quizSession = quizSession(500L, user.getId(), 20L, "hard");
        QuizPlaySession playSession = playSession(700L, quizSession.getId(), user.getId(), quizSession.getSubjectId());
        LocalDate today = today();
        when(userStreakLogRepository.findByUserIdAndStudyDate(user.getId(), today))
                .thenReturn(Optional.of(UserStreakLog.create(user.getId(), today, 1, BigDecimal.valueOf(1.0))));

        QuizRewardResult result = gamificationRewardService.applyQuizReward(user, playSession, quizSession, 3);

        assertThat(result.xpEarned()).isZero();
        assertThat(result.leveledUp()).isFalse();
        assertThat(user.getXpTotal()).isEqualTo(GamificationLevelCalculator.MAX_LEVEL_MIN_XP);
        assertThat(user.getCurrentLevel()).isEqualTo(10);

        // 적립값이 0이면 UserXpLog는 남기지 않는다 (saveRewardLogs 조건)
        verify(userXpLogRepository, never()).save(any(UserXpLog.class));
    }

    @Test
    void applyQuizReward_uses_first_play_xp_table_by_difficulty() {
        // 명세 GAMIF-002 — 첫 풀이 정답 1문항당 easy 3 / medium 4 / hard 5 XP
        assertThat(firstPlayXpForCorrect("easy", 2)).isEqualTo(6);
        assertThat(firstPlayXpForCorrect("medium", 2)).isEqualTo(8);
        assertThat(firstPlayXpForCorrect("hard", 2)).isEqualTo(10);
    }

    @Test
    void applyQuizReward_uses_retry_xp_table_by_difficulty() {
        // 명세 GAMIF-002 — 전체 다시풀기/오답 복습 정답 1문항당 easy 2 / medium 3 / hard 4 XP
        assertThat(retryPlayXpForCorrect("retry_all", "easy", 2)).isEqualTo(4);
        assertThat(retryPlayXpForCorrect("retry_all", "medium", 2)).isEqualTo(6);
        assertThat(retryPlayXpForCorrect("retry_all", "hard", 2)).isEqualTo(8);
        assertThat(retryPlayXpForCorrect("retry_wrong", "easy", 2)).isEqualTo(4);
        assertThat(retryPlayXpForCorrect("retry_wrong", "medium", 2)).isEqualTo(6);
        assertThat(retryPlayXpForCorrect("retry_wrong", "hard", 2)).isEqualTo(8);
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

    private int firstPlayXpForCorrect(String difficulty, int correctCount) {
        User user = user(1L, 0, 0, 1);
        QuizSession quizSession = quizSession(500L, user.getId(), 20L, difficulty);
        QuizPlaySession playSession = playSession(700L, quizSession.getId(), user.getId(), quizSession.getSubjectId());
        stubTodayStreak(user.getId());

        QuizRewardResult result = gamificationRewardService.applyQuizReward(user, playSession, quizSession, correctCount);
        return result.xpEarned();
    }

    private int retryPlayXpForCorrect(String playType, String difficulty, int correctCount) {
        User user = user(1L, 0, 0, 1);
        QuizSession quizSession = quizSession(500L, user.getId(), 20L, difficulty);
        QuizPlaySession playSession = retryPlaySession(700L, playType, quizSession);
        stubTodayStreak(user.getId());

        QuizRewardResult result = gamificationRewardService.applyQuizReward(user, playSession, quizSession, correctCount);
        return result.xpEarned();
    }

    private void stubTodayStreak(Long userId) {
        LocalDate today = today();
        when(userStreakLogRepository.findByUserIdAndStudyDate(userId, today))
                .thenReturn(Optional.of(UserStreakLog.create(userId, today, 1, BigDecimal.valueOf(1.0))));
    }

    private QuizPlaySession retryPlaySession(Long id, String playType, QuizSession quizSession) {
        QuizPlaySession playSession;
        if ("retry_all".equals(playType)) {
            playSession = QuizPlaySession.createRetryAll(
                    "client-session-" + id,
                    quizSession.getId(),
                    quizSession.getUserId(),
                    quizSession.getSubjectId(),
                    false,
                    true,
                    null
            );
        } else if ("retry_wrong".equals(playType)) {
            playSession = QuizPlaySession.createRetryWrong(
                    "client-session-" + id,
                    quizSession.getId(),
                    quizSession.getUserId(),
                    quizSession.getSubjectId(),
                    999L,
                    quizSession.getId(),
                    1,
                    false,
                    true,
                    null
            );
        } else {
            throw new IllegalArgumentException("unsupported playType for retry helper: " + playType);
        }
        ReflectionTestUtils.setField(playSession, "id", id);
        return playSession;
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
