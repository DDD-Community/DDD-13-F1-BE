package com.f1.quiket.domain.gamification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
        name = "user_dotori_logs",
        indexes = {
                @Index(name = "idx_user_dotori_logs_user_id_created_at", columnList = "user_id, created_at"),
                @Index(name = "idx_user_dotori_logs_play_session_id", columnList = "play_session_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserDotoriLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Column(name = "play_session_id")
    Long playSessionId;

    @Column(name = "change_type", length = 20, nullable = false)
    String changeType;

    @Column(name = "amount", nullable = false)
    Integer amount;

    @Column(name = "balance_before", nullable = false)
    Integer balanceBefore;

    @Column(name = "balance_after", nullable = false)
    Integer balanceAfter;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    public static UserDotoriLog createQuizEarn(
            Long userId,
            Long playSessionId,
            Integer amount,
            Integer balanceBefore,
            Integer balanceAfter
    ) {
        UserDotoriLog log = new UserDotoriLog();
        log.userId = userId;
        log.playSessionId = playSessionId;
        log.changeType = "earn_quiz";
        log.amount = amount;
        log.balanceBefore = balanceBefore;
        log.balanceAfter = balanceAfter;
        return log;
    }
}
