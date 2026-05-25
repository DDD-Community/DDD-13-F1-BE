package com.f1.quiket.domain.mypage.dto;

import com.f1.quiket.domain.auth.entity.UserAuthIdentity;
import com.f1.quiket.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyProfileResponse {

    private final String id;
    private final String email;
    private final String nickname;
    private final Integer dotoriBalance;
    private final boolean emailVerified;
    private final String status;
    private final List<String> providers;
    private final Integer xpTotal;
    private final Integer currentLevel;
    private final LocalDateTime createdAt;

    public static MyProfileResponse of(User user, List<UserAuthIdentity> identities) {
        return MyProfileResponse.builder()
                .id(user.getPublicId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .dotoriBalance(user.getDotoriBalance())
                .emailVerified(user.isEmailVerified())
                .status(user.getStatus())
                .providers(identities.stream()
                        .map(UserAuthIdentity::getProvider)
                        .toList())
                .xpTotal(user.getXpTotal())
                .currentLevel(user.getCurrentLevel())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
