package com.f1.quiket.domain.user.dto;

import com.f1.quiket.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

/**
 * 사용자 목록 조회 결과 DTO
 */
@Getter
@Builder
public class UserResponse {

    private final String publicId;
    private final String email;
    private final String nickname;
    private final String status;
    private final boolean emailVerified;
    private final Integer dotoriBalance;
    private final String lastLoginIp;
    private final java.time.LocalDateTime lastLoginAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .publicId(user.getPublicId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .status(user.getStatus())
                .emailVerified(user.isEmailVerified())
                .dotoriBalance(user.getDotoriBalance())
                .lastLoginIp(user.getLastLoginIp())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
