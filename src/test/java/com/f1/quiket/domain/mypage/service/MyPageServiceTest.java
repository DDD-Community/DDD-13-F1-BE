package com.f1.quiket.domain.mypage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.auth.dto.EmailVerificationSentResponse;
import com.f1.quiket.domain.auth.entity.UserAuthIdentity;
import com.f1.quiket.domain.auth.repository.UserAuthIdentityRepository;
import com.f1.quiket.domain.auth.service.EmailVerificationCodeGenerator;
import com.f1.quiket.domain.mypage.dto.MyEmailChangeConfirmRequest;
import com.f1.quiket.domain.mypage.dto.MyEmailChangeRequest;
import com.f1.quiket.domain.mypage.dto.MyProfileResponse;
import com.f1.quiket.domain.mypage.dto.NicknameUpdateRequest;
import com.f1.quiket.domain.mypage.event.MyEmailChangeMailRequestedEvent;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.domain.user.repository.UserRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

class MyPageServiceTest {

    private UserRepository userRepository;
    private UserAuthIdentityRepository userAuthIdentityRepository;
    private EmailVerificationCodeGenerator verificationCodeGenerator;
    private MyEmailChangeVerificationStore emailChangeVerificationStore;
    private ApplicationEventPublisher eventPublisher;
    private MyPageService myPageService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userAuthIdentityRepository = mock(UserAuthIdentityRepository.class);
        verificationCodeGenerator = mock(EmailVerificationCodeGenerator.class);
        emailChangeVerificationStore = mock(MyEmailChangeVerificationStore.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        myPageService = new MyPageService(
                userRepository,
                userAuthIdentityRepository,
                verificationCodeGenerator,
                emailChangeVerificationStore,
                eventPublisher
        );
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

    @Test
    void requestEmailChange_saves_code_and_publishes_mail() {
        User user = user();
        UserAuthIdentity identity = UserAuthIdentity.createLocal(user, "password-hash", true);
        MyEmailChangeRequest request = emailChangeRequest("new.user@example.com");

        when(userRepository.findByPublicIdAndDeletedAtIsNull(user.getPublicId())).thenReturn(Optional.of(user));
        when(userAuthIdentityRepository.findByUserAndProviderAndDeletedAtIsNull(user, "local"))
                .thenReturn(Optional.of(identity));
        when(userRepository.existsByEmail(request.getNewEmail())).thenReturn(false);
        when(verificationCodeGenerator.generate()).thenReturn("123456");

        EmailVerificationSentResponse response = myPageService.requestEmailChange(user.getPublicId(), request);

        assertThat(response.getEmail()).isEqualTo("new.user@example.com");
        assertThat(response.getExpiresInSeconds()).isEqualTo(600L);
        verify(emailChangeVerificationStore).isInCooldown(user.getPublicId());
        verify(emailChangeVerificationStore).save(
                user.getPublicId(),
                new MyEmailChangeVerificationPayload("new.user@example.com", "123456"),
                600L
        );
        verify(eventPublisher).publishEvent(
                new MyEmailChangeMailRequestedEvent("new.user@example.com", "123456")
        );
    }

    @Test
    void requestEmailChange_throws_too_frequent_when_in_cooldown() {
        User user = user();
        UserAuthIdentity identity = UserAuthIdentity.createLocal(user, "password-hash", true);
        MyEmailChangeRequest request = emailChangeRequest("new.user@example.com");

        when(userRepository.findByPublicIdAndDeletedAtIsNull(user.getPublicId())).thenReturn(Optional.of(user));
        when(userAuthIdentityRepository.findByUserAndProviderAndDeletedAtIsNull(user, "local"))
                .thenReturn(Optional.of(identity));
        when(emailChangeVerificationStore.isInCooldown(user.getPublicId())).thenReturn(true);

        assertThatThrownBy(() -> myPageService.requestEmailChange(user.getPublicId(), request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.MY_EMAIL_CHANGE_TOO_FREQUENT);
        verify(emailChangeVerificationStore, never()).save(
                user.getPublicId(),
                new MyEmailChangeVerificationPayload("new.user@example.com", "123456"),
                600L
        );
    }

    @Test
    void requestEmailChange_throws_conflict_when_email_exists() {
        User user = user();
        UserAuthIdentity identity = UserAuthIdentity.createLocal(user, "password-hash", true);
        MyEmailChangeRequest request = emailChangeRequest("new.user@example.com");

        when(userRepository.findByPublicIdAndDeletedAtIsNull(user.getPublicId())).thenReturn(Optional.of(user));
        when(userAuthIdentityRepository.findByUserAndProviderAndDeletedAtIsNull(user, "local"))
                .thenReturn(Optional.of(identity));
        when(userRepository.existsByEmail(request.getNewEmail())).thenReturn(true);

        assertThatThrownBy(() -> myPageService.requestEmailChange(user.getPublicId(), request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_EMAIL_ALREADY_EXISTS);
        verify(emailChangeVerificationStore, never()).save(
                user.getPublicId(),
                new MyEmailChangeVerificationPayload("new.user@example.com", "123456"),
                600L
        );
    }

    @Test
    void requestEmailChange_throws_conflict_when_local_identity_missing() {
        User user = user();
        MyEmailChangeRequest request = emailChangeRequest("new.user@example.com");

        when(userRepository.findByPublicIdAndDeletedAtIsNull(user.getPublicId())).thenReturn(Optional.of(user));
        when(userAuthIdentityRepository.findByUserAndProviderAndDeletedAtIsNull(user, "local"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> myPageService.requestEmailChange(user.getPublicId(), request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_LOCAL_IDENTITY_NOT_FOUND);
    }

    @Test
    void confirmEmailChange_updates_email_and_deletes_code() {
        User user = user();
        UserAuthIdentity identity = UserAuthIdentity.createLocal(user, "password-hash", true);
        MyEmailChangeConfirmRequest request = emailChangeConfirmRequest("new.user@example.com", "123456");

        when(userRepository.findByPublicIdAndDeletedAtIsNull(user.getPublicId())).thenReturn(Optional.of(user));
        when(userAuthIdentityRepository.findByUserAndProviderAndDeletedAtIsNull(user, "local"))
                .thenReturn(Optional.of(identity));
        when(userRepository.existsByEmail(request.getNewEmail())).thenReturn(false);
        when(emailChangeVerificationStore.find(user.getPublicId()))
                .thenReturn(Optional.of(new MyEmailChangeVerificationPayload("new.user@example.com", "123456")));
        when(userAuthIdentityRepository.findAllByUserAndDeletedAtIsNull(user)).thenReturn(List.of(identity));

        MyProfileResponse response = myPageService.confirmEmailChange(user.getPublicId(), request);

        assertThat(user.getEmail()).isEqualTo("new.user@example.com");
        assertThat(response.getEmail()).isEqualTo("new.user@example.com");
        verify(emailChangeVerificationStore).delete(user.getPublicId());
        verify(emailChangeVerificationStore).markCooldown(user.getPublicId(), 86_400L);
    }

    @Test
    void confirmEmailChange_throws_expired_when_code_missing() {
        User user = user();
        UserAuthIdentity identity = UserAuthIdentity.createLocal(user, "password-hash", true);
        MyEmailChangeConfirmRequest request = emailChangeConfirmRequest("new.user@example.com", "123456");

        when(userRepository.findByPublicIdAndDeletedAtIsNull(user.getPublicId())).thenReturn(Optional.of(user));
        when(userAuthIdentityRepository.findByUserAndProviderAndDeletedAtIsNull(user, "local"))
                .thenReturn(Optional.of(identity));
        when(userRepository.existsByEmail(request.getNewEmail())).thenReturn(false);
        when(emailChangeVerificationStore.find(user.getPublicId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> myPageService.confirmEmailChange(user.getPublicId(), request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_EMAIL_VERIFICATION_EXPIRED);
    }

    @Test
    void confirmEmailChange_throws_invalid_when_code_mismatched() {
        User user = user();
        UserAuthIdentity identity = UserAuthIdentity.createLocal(user, "password-hash", true);
        MyEmailChangeConfirmRequest request = emailChangeConfirmRequest("new.user@example.com", "000000");

        when(userRepository.findByPublicIdAndDeletedAtIsNull(user.getPublicId())).thenReturn(Optional.of(user));
        when(userAuthIdentityRepository.findByUserAndProviderAndDeletedAtIsNull(user, "local"))
                .thenReturn(Optional.of(identity));
        when(userRepository.existsByEmail(request.getNewEmail())).thenReturn(false);
        when(emailChangeVerificationStore.find(user.getPublicId()))
                .thenReturn(Optional.of(new MyEmailChangeVerificationPayload("new.user@example.com", "123456")));

        assertThatThrownBy(() -> myPageService.confirmEmailChange(user.getPublicId(), request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_INVALID_EMAIL_VERIFICATION);
        verify(emailChangeVerificationStore, never()).delete(user.getPublicId());
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

    private MyEmailChangeRequest emailChangeRequest(String newEmail) {
        MyEmailChangeRequest request = new MyEmailChangeRequest();
        ReflectionTestUtils.setField(request, "newEmail", newEmail);
        return request;
    }

    private MyEmailChangeConfirmRequest emailChangeConfirmRequest(String newEmail, String verificationCode) {
        MyEmailChangeConfirmRequest request = new MyEmailChangeConfirmRequest();
        ReflectionTestUtils.setField(request, "newEmail", newEmail);
        ReflectionTestUtils.setField(request, "verificationCode", verificationCode);
        return request;
    }
}
