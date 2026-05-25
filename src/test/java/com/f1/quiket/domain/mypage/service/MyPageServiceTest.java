package com.f1.quiket.domain.mypage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.auth.entity.UserAuthIdentity;
import com.f1.quiket.domain.auth.repository.UserAuthIdentityRepository;
import com.f1.quiket.domain.mypage.dto.MyProfileResponse;
import com.f1.quiket.domain.mypage.dto.NicknameUpdateRequest;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.domain.user.repository.UserRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MyPageServiceTest {

    private UserRepository userRepository;
    private UserAuthIdentityRepository userAuthIdentityRepository;
    private MyPageService myPageService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userAuthIdentityRepository = mock(UserAuthIdentityRepository.class);
        myPageService = new MyPageService(userRepository, userAuthIdentityRepository);
    }

    @Test
    void getMyProfile_returns_profile() {
        User user = user();
        UserAuthIdentity localIdentity = UserAuthIdentity.createLocal(user, "password-hash", true);
        UserAuthIdentity kakaoIdentity = UserAuthIdentity.createOAuth(user, "kakao", "kakao-subject", false);

        when(userRepository.findByPublicIdAndDeletedAtIsNull(user.getPublicId())).thenReturn(Optional.of(user));
        when(userAuthIdentityRepository.findAllByUserAndDeletedAtIsNull(user))
                .thenReturn(List.of(localIdentity, kakaoIdentity));

        MyProfileResponse response = myPageService.getMyProfile(user.getPublicId());

        assertThat(response.getId()).isEqualTo(user.getPublicId());
        assertThat(response.getEmail()).isEqualTo("user@example.com");
        assertThat(response.getNickname()).isEqualTo("도토리");
        assertThat(response.getDotoriBalance()).isEqualTo(12);
        assertThat(response.isEmailVerified()).isTrue();
        assertThat(response.getStatus()).isEqualTo("active");
        assertThat(response.getProviders()).containsExactly("local", "kakao");
        assertThat(response.getXpTotal()).isEqualTo(360);
        assertThat(response.getCurrentLevel()).isEqualTo(3);
        assertThat(response.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 5, 19, 7, 30));
    }

    @Test
    void updateNickname_changes_nickname() {
        User user = user();
        NicknameUpdateRequest request = nicknameUpdateRequest("새닉네임");
        UserAuthIdentity identity = UserAuthIdentity.createLocal(user, "password-hash", true);

        when(userRepository.findByPublicIdAndDeletedAtIsNull(user.getPublicId())).thenReturn(Optional.of(user));
        when(userAuthIdentityRepository.findAllByUserAndDeletedAtIsNull(user)).thenReturn(List.of(identity));

        MyProfileResponse response = myPageService.updateNickname(user.getPublicId(), request);

        assertThat(user.getNickname()).isEqualTo("새닉네임");
        assertThat(response.getNickname()).isEqualTo("새닉네임");
        assertThat(response.getProviders()).containsExactly("local");
    }

    @Test
    void getMyProfile_throws_not_found_when_user_missing() {
        String userPublicId = "018f8c2e-5f73-7b6a-b9f0-3f55e7f7c901";
        when(userRepository.findByPublicIdAndDeletedAtIsNull(userPublicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> myPageService.getMyProfile(userPublicId))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_USER_NOT_FOUND);
    }

    private User user() {
        User user = User.create("018f8c2e-5f73-7b6a-b9f0-3f55e7f7c901", "user@example.com", "도토리");
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(user, "emailVerified", true);
        ReflectionTestUtils.setField(user, "dotoriBalance", 12);
        ReflectionTestUtils.setField(user, "xpTotal", 360);
        ReflectionTestUtils.setField(user, "currentLevel", 3);
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.of(2026, 5, 19, 7, 30));
        return user;
    }

    private NicknameUpdateRequest nicknameUpdateRequest(String nickname) {
        NicknameUpdateRequest request = new NicknameUpdateRequest();
        ReflectionTestUtils.setField(request, "nickname", nickname);
        return request;
    }
}
