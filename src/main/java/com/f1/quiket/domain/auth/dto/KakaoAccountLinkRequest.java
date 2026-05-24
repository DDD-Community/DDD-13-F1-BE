package com.f1.quiket.domain.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoAccountLinkRequest {

    @NotBlank(message = "계정 연동 토큰은 필수입니다")
    private String linkToken;

    @Email(message = "올바른 이메일 형식이 아닙니다")
    @NotBlank(message = "이메일은 필수입니다")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;

    @AssertTrue(message = "계정 연동 동의는 필수입니다")
    private boolean agreedToLink;
}
