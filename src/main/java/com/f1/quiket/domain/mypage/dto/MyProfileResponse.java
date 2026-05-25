package com.f1.quiket.domain.mypage.dto;

import com.f1.quiket.domain.auth.entity.UserAuthIdentity;
import com.f1.quiket.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 마이페이지 계정 정보 응답 DTO (기능명세 MY-001)
 */
@Getter
@Builder
public class MyProfileResponse {

    // primary 인증 수단 우선, 그 다음 provider명 알파벳순 — 응답 순서 결정성 보장
    private static final Comparator<UserAuthIdentity> PROVIDER_ORDER =
            Comparator.comparing(UserAuthIdentity::isPrimary).reversed()
                    .thenComparing(UserAuthIdentity::getProvider);

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
                        .sorted(PROVIDER_ORDER)
                        .map(UserAuthIdentity::getProvider)
                        .toList())
                .xpTotal(user.getXpTotal())
                .currentLevel(user.getCurrentLevel())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
