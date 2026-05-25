package com.f1.quiket.domain.gamification.service;

import com.f1.quiket.domain.gamification.dto.GamificationResponse;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.domain.user.repository.UserRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GamificationService {

    private final UserRepository userRepository;

    public GamificationResponse getMyGamification(String userPublicId) {
        User user = userRepository.findByPublicIdAndDeletedAtIsNull(userPublicId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_USER_NOT_FOUND));
        return GamificationResponse.from(user);
    }
}
