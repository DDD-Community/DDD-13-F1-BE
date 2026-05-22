package com.f1.quiket.domain.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Getter
@NoArgsConstructor
public class PasswordResetConfirmRequest {

    @Email(message = "올바른 이메일 형식이 아닙니다")
    @NotBlank(message = "이메일은 필수입니다")
    @Size(max = 255, message = "이메일은 255자 이하여야 합니다")
    private String email;

    private String resetToken;

    private String verificationCode;

    @NotBlank(message = "새 비밀번호는 필수입니다")
    @Size(min = 8, max = 32, message = "새 비밀번호는 8자 이상 32자 이하여야 합니다")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z0-9~!@#$%^&*()_+=\\[\\]{}.,?-]{8,32}$",
            message = "비밀번호는 영문과 숫자를 포함하고 공백 없이 허용된 문자만 사용할 수 있습니다")
    private String newPassword;

    @NotBlank(message = "새 비밀번호 확인은 필수입니다")
    @Size(min = 8, max = 32, message = "새 비밀번호 확인은 8자 이상 32자 이하여야 합니다")
    private String newPasswordConfirm;

    @AssertTrue(message = "인증 코드 또는 재설정 토큰은 필수입니다")
    public boolean isResetCredentialPresent() {
        return StringUtils.hasText(resetToken) || StringUtils.hasText(verificationCode);
    }
}
