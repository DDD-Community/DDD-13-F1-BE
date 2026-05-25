package com.f1.quiket.domain.mypage.service;

import com.f1.quiket.domain.auth.repository.UserAuthIdentityRepository;
import com.f1.quiket.domain.mypage.dto.MyProfileResponse;
import com.f1.quiket.domain.mypage.dto.NicknameUpdateRequest;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.domain.user.repository.UserRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MyPageService {

    private final UserRepository userRepository;
    private final UserAuthIdentityRepository userAuthIdentityRepository;

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

    private User findActiveUser(String userPublicId) {
        return userRepository.findByPublicIdAndDeletedAtIsNull(userPublicId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_USER_NOT_FOUND));
    }

    private MyProfileResponse toMyProfileResponse(User user) {
        return MyProfileResponse.of(user, userAuthIdentityRepository.findAllByUserAndDeletedAtIsNull(user));
    }
}
