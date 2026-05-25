package com.f1.quiket.domain.mypage.service;

import com.f1.quiket.domain.auth.dto.EmailVerificationSentResponse;
import com.f1.quiket.domain.auth.entity.UserAuthIdentity;
import com.f1.quiket.domain.auth.repository.UserAuthIdentityRepository;
import com.f1.quiket.domain.auth.service.AuthTokenService;
import com.f1.quiket.domain.auth.service.EmailVerificationCodeGenerator;
import com.f1.quiket.domain.mypage.dto.MyAccountDeleteRequest;
import com.f1.quiket.domain.mypage.dto.MyEmailChangeConfirmRequest;
import com.f1.quiket.domain.mypage.dto.MyEmailChangeRequest;
import com.f1.quiket.domain.mypage.dto.MyPasswordChangeRequest;
import com.f1.quiket.domain.mypage.dto.MyProfileResponse;
import com.f1.quiket.domain.mypage.dto.NicknameUpdateRequest;
import com.f1.quiket.domain.mypage.event.MyEmailChangeMailRequestedEvent;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.domain.user.repository.UserRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class MyPageService {

    private static final String PROVIDER_LOCAL = "local";
    private static final long EMAIL_CHANGE_TTL_SECONDS = 600L;
    private static final long EMAIL_CHANGE_COOLDOWN_SECONDS = 86_400L;

    private final UserRepository userRepository;
    private final UserAuthIdentityRepository userAuthIdentityRepository;
    private final EmailVerificationCodeGenerator verificationCodeGenerator;
    private final MyEmailChangeVerificationStore emailChangeVerificationStore;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;

    @Transactional(readOnly = true)
    public MyProfileResponse getMyProfile(String userPublicId) {
        User user = findActiveUser(userPublicId);
        return toMyProfileResponse(user);
    }

    public MyProfileResponse updateNickname(String userPublicId, NicknameUpdateRequest request) {
        User user = findActiveUser(userPublicId);
        user.changeNickname(request.getNickname());
        return toMyProfileResponse(user);
    }

    public EmailVerificationSentResponse requestEmailChange(String userPublicId, MyEmailChangeRequest request) {
        User user = findActiveUser(userPublicId);
        findLocalIdentity(user);
        validateNotInCooldown(userPublicId);
        validateEmailNotExists(request.getNewEmail());

        String verificationCode = verificationCodeGenerator.generate();
        emailChangeVerificationStore.save(
                userPublicId,
                new MyEmailChangeVerificationPayload(request.getNewEmail(), verificationCode),
                EMAIL_CHANGE_TTL_SECONDS
        );
        eventPublisher.publishEvent(new MyEmailChangeMailRequestedEvent(request.getNewEmail(), verificationCode));
        return EmailVerificationSentResponse.of(request.getNewEmail(), EMAIL_CHANGE_TTL_SECONDS);
    }

    public MyProfileResponse confirmEmailChange(String userPublicId, MyEmailChangeConfirmRequest request) {
        User user = findActiveUser(userPublicId);
        findLocalIdentity(user);
        validateEmailNotExists(request.getNewEmail());

        MyEmailChangeVerificationPayload payload = emailChangeVerificationStore.find(userPublicId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_EMAIL_VERIFICATION_EXPIRED));
        if (!payload.newEmail().equals(request.getNewEmail())
                || !payload.verificationCode().equals(request.getVerificationCode())) {
            throw new CustomException(ErrorCode.AUTH_INVALID_EMAIL_VERIFICATION);
        }

        user.changeEmail(request.getNewEmail());
        emailChangeVerificationStore.delete(userPublicId);
        // 기능명세 MY-001 정책 — 이메일 변경 성공 시 하루 cool-down 시작
        emailChangeVerificationStore.markCooldown(userPublicId, EMAIL_CHANGE_COOLDOWN_SECONDS);
        return toMyProfileResponse(user);
    }

    public void updatePassword(String userPublicId, MyPasswordChangeRequest request) {
        validatePasswordConfirm(request.getNewPassword(), request.getNewPasswordConfirm());

        User user = findActiveUser(userPublicId);
        UserAuthIdentity localIdentity = findLocalIdentity(user);
        validateCurrentPassword(localIdentity, request.getCurrentPassword());
        validateNewPassword(localIdentity, request.getNewPassword());

        localIdentity.changePassword(passwordEncoder.encode(request.getNewPassword()));
        authTokenService.revokeAllActiveRefreshTokens(user);
    }

    public void deleteAccount(String userPublicId, MyAccountDeleteRequest request) {
        validateDeleteAgreement(request);

        User user = findActiveUser(userPublicId);
        List<UserAuthIdentity> identities = userAuthIdentityRepository.findAllByUserAndDeletedAtIsNull(user);
        findLocalIdentity(identities).ifPresent(localIdentity ->
                validateAccountDeletePassword(localIdentity, request.getPassword()));

        authTokenService.revokeAllActiveRefreshTokens(user);
        identities.forEach(UserAuthIdentity::delete);
        user.delete();
    }

    private User findActiveUser(String userPublicId) {
        return userRepository.findByPublicIdAndDeletedAtIsNull(userPublicId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_USER_NOT_FOUND));
    }

    private UserAuthIdentity findLocalIdentity(User user) {
        UserAuthIdentity localIdentity = userAuthIdentityRepository
                .findByUserAndProviderAndDeletedAtIsNull(user, PROVIDER_LOCAL)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_LOCAL_IDENTITY_NOT_FOUND));

        if (!StringUtils.hasText(localIdentity.getPasswordHash())) {
            throw new CustomException(ErrorCode.AUTH_LOCAL_IDENTITY_NOT_FOUND);
        }
        return localIdentity;
    }

    private Optional<UserAuthIdentity> findLocalIdentity(List<UserAuthIdentity> identities) {
        return identities.stream()
                .filter(identity -> PROVIDER_LOCAL.equals(identity.getProvider()))
                .findFirst();
    }

    private void validateEmailNotExists(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.AUTH_EMAIL_ALREADY_EXISTS);
        }
    }

    private void validateNotInCooldown(String userPublicId) {
        if (emailChangeVerificationStore.isInCooldown(userPublicId)) {
            throw new CustomException(ErrorCode.MY_EMAIL_CHANGE_TOO_FREQUENT);
        }
    }

    private void validatePasswordConfirm(String password, String passwordConfirm) {
        if (!password.equals(passwordConfirm)) {
            throw new CustomException(ErrorCode.AUTH_PASSWORD_CONFIRM_MISMATCH);
        }
    }

    private void validateCurrentPassword(UserAuthIdentity localIdentity, String currentPassword) {
        if (!passwordEncoder.matches(currentPassword, localIdentity.getPasswordHash())) {
            throw new CustomException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
    }

    private void validateNewPassword(UserAuthIdentity localIdentity, String newPassword) {
        if (passwordEncoder.matches(newPassword, localIdentity.getPasswordHash())) {
            throw new CustomException(ErrorCode.AUTH_PASSWORD_SAME_AS_PREVIOUS);
        }
    }

    private void validateDeleteAgreement(MyAccountDeleteRequest request) {
        if (!request.isAgreedToDelete()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "회원 탈퇴 동의는 필수입니다.");
        }
    }

    private void validateAccountDeletePassword(UserAuthIdentity localIdentity, String password) {
        if (!StringUtils.hasText(localIdentity.getPasswordHash())) {
            throw new CustomException(ErrorCode.AUTH_LOCAL_IDENTITY_NOT_FOUND);
        }
        if (!StringUtils.hasText(password) || !passwordEncoder.matches(password, localIdentity.getPasswordHash())) {
            throw new CustomException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
    }

    private MyProfileResponse toMyProfileResponse(User user) {
        return MyProfileResponse.of(user, userAuthIdentityRepository.findAllByUserAndDeletedAtIsNull(user));
    }
}
