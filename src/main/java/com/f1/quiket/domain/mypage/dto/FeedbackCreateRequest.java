package com.f1.quiket.domain.mypage.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FeedbackCreateRequest {

    @NotBlank(message = "피드백 카테고리는 필수입니다")
    @Pattern(regexp = "feature|bug|inquiry|other", message = "피드백 카테고리가 올바르지 않습니다")
    private String category;

    @NotBlank(message = "피드백 내용은 필수입니다")
    @Size(max = 1000, message = "피드백 내용은 1000자 이하여야 합니다")
    private String body;

    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Size(max = 255, message = "회신 이메일은 255자 이하여야 합니다")
    private String replyEmail;

    @Size(max = 20, message = "앱 버전은 20자 이하여야 합니다")
    private String appVersion;

    @Size(max = 50, message = "OS 버전은 50자 이하여야 합니다")
    private String osVersion;

    @Size(max = 100, message = "디바이스 모델은 100자 이하여야 합니다")
    private String deviceModel;
}
