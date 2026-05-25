package com.f1.quiket.domain.mypage.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class MyPageService {

    private static final String PROVIDER_LOCAL = "local";
    private static final long EMAIL_CHANGE_TTL_SECONDS = 600L;

    private final UserRepository userRepository;
    private final UserAuthIdentityRepository userAuthIdentityRepository;
    private final EmailVerificationCodeGenerator verificationCodeGenerator;
    private final MyEmailChangeVerificationStore emailChangeVerificationStore;
    private final ApplicationEventPublisher eventPublisher;

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
        return toMyProfileResponse(user);
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

    private void validateEmailNotExists(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.AUTH_EMAIL_ALREADY_EXISTS);
        }
    }

    private MyProfileResponse toMyProfileResponse(User user) {
        return MyProfileResponse.of(user, userAuthIdentityRepository.findAllByUserAndDeletedAtIsNull(user));
    }
}
