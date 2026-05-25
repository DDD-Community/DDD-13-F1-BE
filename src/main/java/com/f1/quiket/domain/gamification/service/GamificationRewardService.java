package com.f1.quiket.domain.gamification.service;

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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class GamificationRewardService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String PLAY_TYPE_FIRST = "first";
    private static final String XP_TYPE_QUIZ_CORRECT = "quiz_correct";
    private static final String XP_TYPE_QUIZ_RETRY = "quiz_retry";

    private final UserXpLogRepository userXpLogRepository;
    private final UserDotoriLogRepository userDotoriLogRepository;
    private final UserStreakLogRepository userStreakLogRepository;

    public QuizRewardResult applyQuizReward(
            User user,
            QuizPlaySession playSession,
            QuizSession quizSession,
            Integer correctCount
    ) {
        int normalizedCorrectCount = Math.max(correctCount == null ? 0 : correctCount, 0);
        UserStreakLog streakLog = findOrCreateTodayStreak(user.getId());
        BigDecimal streakMultiplier = streakLog.getMultiplier();

        int dotoriEarned = calculateDotoriEarned(playSession, normalizedCorrectCount);
        int baseXp = normalizedCorrectCount * xpPerCorrect(quizSession.getDifficulty(), playSession.getPlayType());
        int earnedXp = calculateEarnedXp(baseXp, streakMultiplier);

        int dotoriBefore = user.getDotoriBalance();
        int xpBefore = user.getXpTotal();
        int levelBefore = user.getCurrentLevel();
        int dotoriAfter = dotoriBefore + dotoriEarned;
        int xpAfter = xpBefore + earnedXp;
        int levelAfter = GamificationLevelCalculator.levelOf(xpAfter);
        boolean leveledUp = levelAfter > levelBefore;

        user.applyQuizReward(dotoriEarned, earnedXp, levelAfter);
        saveRewardLogs(
                user,
                playSession,
                dotoriEarned,
                dotoriBefore,
                dotoriAfter,
                baseXp,
                streakMultiplier,
                earnedXp,
                xpBefore,
                xpAfter,
                levelBefore,
                levelAfter
        );

        return new QuizRewardResult(
                dotoriEarned,
                earnedXp,
                leveledUp,
                leveledUp ? levelAfter : null,
                user.getDotoriBalance(),
                user.getXpTotal()
        );
    }

    private UserStreakLog findOrCreateTodayStreak(Long userId) {
        LocalDate today = LocalDate.now(KST);
        return userStreakLogRepository.findByUserIdAndStudyDate(userId, today)
                .orElseGet(() -> createTodayStreak(userId, today));
    }

    private UserStreakLog createTodayStreak(Long userId, LocalDate today) {
        int streakCount = userStreakLogRepository.findTopByUserIdAndStudyDateBeforeOrderByStudyDateDesc(userId, today)
                .map(previous -> previous.getStudyDate().equals(today.minusDays(1))
                        ? previous.getStreakCount() + 1
                        : 1)
                .orElse(1);
        return userStreakLogRepository.save(UserStreakLog.create(userId, today, streakCount, multiplierOf(streakCount)));
    }

    private int calculateDotoriEarned(QuizPlaySession playSession, int correctCount) {
        if (!PLAY_TYPE_FIRST.equals(playSession.getPlayType())) {
            return 0;
        }
        return correctCount;
    }

    private int xpPerCorrect(String difficulty, String playType) {
        if (PLAY_TYPE_FIRST.equals(playType)) {
            return switch (difficulty) {
                case "easy" -> 3;
                case "hard" -> 5;
                default -> 4;
            };
        }
        return switch (difficulty) {
            case "easy" -> 2;
            case "hard" -> 4;
            default -> 3;
        };
    }

    private BigDecimal multiplierOf(int streakCount) {
        if (streakCount >= 30) {
            return BigDecimal.valueOf(2.5);
        }
        if (streakCount >= 14) {
            return BigDecimal.valueOf(2.0);
        }
        if (streakCount >= 6) {
            return BigDecimal.valueOf(1.5);
        }
        if (streakCount >= 3) {
            return BigDecimal.valueOf(1.2);
        }
        if (streakCount == 2) {
            return BigDecimal.valueOf(1.1);
        }
        return BigDecimal.valueOf(1.0);
    }

    private int calculateEarnedXp(int baseXp, BigDecimal streakMultiplier) {
        return BigDecimal.valueOf(baseXp)
                .multiply(streakMultiplier)
                .setScale(0, RoundingMode.DOWN)
                .intValue();
    }

    private void saveRewardLogs(
            User user,
            QuizPlaySession playSession,
            int dotoriEarned,
            int dotoriBefore,
            int dotoriAfter,
            int baseXp,
            BigDecimal streakMultiplier,
            int earnedXp,
            int xpBefore,
            int xpAfter,
            int levelBefore,
            int levelAfter
    ) {
        if (dotoriEarned > 0) {
            userDotoriLogRepository.save(UserDotoriLog.createQuizEarn(
                    user.getId(),
                    playSession.getId(),
                    dotoriEarned,
                    dotoriBefore,
                    dotoriAfter
            ));
        }
        if (earnedXp > 0) {
            userXpLogRepository.save(UserXpLog.createQuizReward(
                    user.getId(),
                    playSession.getId(),
                    PLAY_TYPE_FIRST.equals(playSession.getPlayType()) ? XP_TYPE_QUIZ_CORRECT : XP_TYPE_QUIZ_RETRY,
                    baseXp,
                    streakMultiplier,
                    earnedXp,
                    xpBefore,
                    xpAfter,
                    levelBefore,
                    levelAfter
            ));
        }
    }
}
