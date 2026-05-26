package com.f1.quiket.domain.mypage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NicknameUpdateRequest {

    @NotBlank(message = "닉네임은 필수입니다")
    @Size(min = 2, max = 12, message = "닉네임은 2자 이상 12자 이하여야 합니다")
    @Pattern(regexp = "^[가-힣A-Za-z]{2,12}$", message = "닉네임은 한글 또는 영문만 사용할 수 있습니다")
    private String nickname;
}
