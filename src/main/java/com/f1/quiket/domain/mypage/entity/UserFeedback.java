package com.f1.quiket.domain.mypage.entity;

import com.f1.quiket.domain.mypage.dto.FeedbackCreateRequest;
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
        name = "user_feedbacks",
        indexes = {
                @Index(name = "idx_user_feedbacks_user_id", columnList = "user_id"),
                @Index(name = "idx_user_feedbacks_category_created_at", columnList = "category, created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Column(name = "category", length = 20, nullable = false)
    String category;

    @Column(name = "body", length = 1000, nullable = false)
    String body;

    @Column(name = "reply_email", length = 255)
    String replyEmail;

    @Column(name = "app_version", length = 20)
    String appVersion;

    @Column(name = "os_version", length = 50)
    String osVersion;

    @Column(name = "device_model", length = 100)
    String deviceModel;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(name = "deleted_at")
    LocalDateTime deletedAt;

    public static UserFeedback create(Long userId, FeedbackCreateRequest request) {
        UserFeedback feedback = new UserFeedback();
        feedback.userId = userId;
        feedback.category = request.getCategory();
        feedback.body = request.getBody();
        feedback.replyEmail = request.getReplyEmail();
        feedback.appVersion = request.getAppVersion();
        feedback.osVersion = request.getOsVersion();
        feedback.deviceModel = request.getDeviceModel();
        feedback.deletedAt = null;
        return feedback;
    }
}
