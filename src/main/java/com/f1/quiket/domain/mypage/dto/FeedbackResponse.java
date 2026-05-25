package com.f1.quiket.domain.mypage.dto;

import com.f1.quiket.domain.mypage.entity.UserFeedback;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FeedbackResponse {

    private final String id;
    private final String category;
    private final String body;
    private final String replyEmail;
    private final String appVersion;
    private final String osVersion;
    private final String deviceModel;
    private final LocalDateTime createdAt;

    public static FeedbackResponse from(UserFeedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId().toString())
                .category(feedback.getCategory())
                .body(feedback.getBody())
                .replyEmail(feedback.getReplyEmail())
                .appVersion(feedback.getAppVersion())
                .osVersion(feedback.getOsVersion())
                .deviceModel(feedback.getDeviceModel())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}
