package com.f1.quiket.domain.gamification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
        name = "user_streak_logs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_user_streak_logs_user_study_date", columnNames = {"user_id", "study_date"})
        },
        indexes = {
                @Index(name = "idx_user_streak_logs_user_id_study_date", columnList = "user_id, study_date")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserStreakLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Column(name = "study_date", nullable = false)
    LocalDate studyDate;

    @Column(name = "streak_count", nullable = false)
    Integer streakCount;

    @Column(name = "multiplier", precision = 3, scale = 1, nullable = false)
    BigDecimal multiplier;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    public static UserStreakLog create(
            Long userId,
            LocalDate studyDate,
            Integer streakCount,
            BigDecimal multiplier
    ) {
        UserStreakLog log = new UserStreakLog();
        log.userId = userId;
        log.studyDate = studyDate;
        log.streakCount = streakCount;
        log.multiplier = multiplier;
        return log;
    }
}
