package com.f1.quiket.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AppleLoginRequest {

    @NotBlank(message = "Apple Identity Token은 필수입니다")
    private String identityToken;

    /** 탈퇴 시 revoke용 refresh token 교환에 사용 - 최초 로그인 시 전달 권장 */
    @Size(max = 512, message = "Authorization Code는 512자 이하여야 합니다")
    private String authorizationCode;

    /** Apple 최초 인가 시에만 클라이언트에 제공되는 사용자 이름 - 닉네임 제안에 사용 */
    @Size(max = 100, message = "이름은 100자 이하여야 합니다")
    private String fullName;

    private Boolean agreedToTerms;
}
