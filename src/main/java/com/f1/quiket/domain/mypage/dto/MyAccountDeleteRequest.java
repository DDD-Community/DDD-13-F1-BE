package com.f1.quiket.domain.mypage.dto;

import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MyAccountDeleteRequest {

    private String password;

    @AssertTrue(message = "회원 탈퇴 동의는 필수입니다")
    private boolean agreedToDelete;
}
