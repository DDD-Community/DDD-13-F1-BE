package com.f1.quiket.domain.mypage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MyPasswordChangeRequest {

    @NotBlank(message = "현재 비밀번호는 필수입니다")
    private String currentPassword;

    @NotBlank(message = "새 비밀번호는 필수입니다")
    @Size(min = 8, max = 32, message = "새 비밀번호는 8자 이상 32자 이하여야 합니다")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z0-9~!@#$%^&*()_+=\\[\\]{}.,?-]{8,32}$",
            message = "새 비밀번호는 영문과 숫자를 포함하고 공백 없이 허용된 문자만 사용할 수 있습니다")
    private String newPassword;

    @NotBlank(message = "새 비밀번호 확인은 필수입니다")
    @Size(min = 8, max = 32, message = "새 비밀번호 확인은 8자 이상 32자 이하여야 합니다")
    private String newPasswordConfirm;
}
