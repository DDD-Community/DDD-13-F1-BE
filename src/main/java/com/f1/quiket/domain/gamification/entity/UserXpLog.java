package com.f1.quiket.domain.gamification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
        name = "user_xp_logs",
        indexes = {
                @Index(name = "idx_user_xp_logs_user_id_created_at", columnList = "user_id, created_at"),
                @Index(name = "idx_user_xp_logs_play_session_id", columnList = "play_session_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserXpLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Column(name = "play_session_id")
    Long playSessionId;

    @Column(name = "lecture_upload_id")
    Long lectureUploadId;

    @Column(name = "xp_type", length = 30, nullable = false)
    String xpType;

    @Column(name = "base_xp", nullable = false)
    Integer baseXp;

    @Column(name = "streak_multiplier", precision = 3, scale = 1, nullable = false)
    BigDecimal streakMultiplier;

    @Column(name = "earned_xp", nullable = false)
    Integer earnedXp;

    @Column(name = "xp_before", nullable = false)
    Integer xpBefore;

    @Column(name = "xp_after", nullable = false)
    Integer xpAfter;

    @Column(name = "level_before", nullable = false)
    Integer levelBefore;

    @Column(name = "level_after", nullable = false)
    Integer levelAfter;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    public static UserXpLog createQuizReward(
            Long userId,
            Long playSessionId,
            String xpType,
            Integer baseXp,
            BigDecimal streakMultiplier,
            Integer earnedXp,
            Integer xpBefore,
            Integer xpAfter,
            Integer levelBefore,
            Integer levelAfter
    ) {
        UserXpLog log = new UserXpLog();
        log.userId = userId;
        log.playSessionId = playSessionId;
        log.xpType = xpType;
        log.baseXp = baseXp;
        log.streakMultiplier = streakMultiplier;
        log.earnedXp = earnedXp;
        log.xpBefore = xpBefore;
        log.xpAfter = xpAfter;
        log.levelBefore = levelBefore;
        log.levelAfter = levelAfter;
        return log;
    }
}
