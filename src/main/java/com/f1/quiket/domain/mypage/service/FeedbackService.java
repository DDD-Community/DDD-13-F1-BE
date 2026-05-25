package com.f1.quiket.domain.mypage.service;

import com.f1.quiket.domain.mypage.dto.FeedbackCreateRequest;
import com.f1.quiket.domain.mypage.dto.FeedbackResponse;
import com.f1.quiket.domain.mypage.entity.UserFeedback;
import com.f1.quiket.domain.mypage.repository.UserFeedbackRepository;
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
public class FeedbackService {

    private final UserRepository userRepository;
    private final UserFeedbackRepository userFeedbackRepository;

    public FeedbackResponse createFeedback(String userPublicId, FeedbackCreateRequest request) {
        User user = findActiveUser(userPublicId);
        UserFeedback feedback = userFeedbackRepository.save(UserFeedback.create(user.getId(), request));
        return FeedbackResponse.from(feedback);
    }

    private User findActiveUser(String userPublicId) {
        return userRepository.findByPublicIdAndDeletedAtIsNull(userPublicId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_USER_NOT_FOUND));
    }
}
