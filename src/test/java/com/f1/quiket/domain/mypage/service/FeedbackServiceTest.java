package com.f1.quiket.domain.mypage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.mypage.dto.FeedbackCreateRequest;
import com.f1.quiket.domain.mypage.dto.FeedbackResponse;
import com.f1.quiket.domain.mypage.entity.UserFeedback;
import com.f1.quiket.domain.mypage.repository.UserFeedbackRepository;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.domain.user.repository.UserRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class FeedbackServiceTest {

    private UserRepository userRepository;
    private UserFeedbackRepository userFeedbackRepository;
    private FeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userFeedbackRepository = mock(UserFeedbackRepository.class);
        feedbackService = new FeedbackService(userRepository, userFeedbackRepository);
    }

    @Test
    void createFeedback_saves_feedback_and_returns_response() {
        User user = user();
        FeedbackCreateRequest request = feedbackCreateRequest();
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 25, 9, 30);

        when(userRepository.findByPublicIdAndDeletedAtIsNull(user.getPublicId())).thenReturn(Optional.of(user));
        when(userFeedbackRepository.save(any(UserFeedback.class)))
                .thenAnswer(invocation -> {
                    UserFeedback feedback = invocation.getArgument(0);
                    ReflectionTestUtils.setField(feedback, "id", 10L);
                    ReflectionTestUtils.setField(feedback, "createdAt", createdAt);
                    return feedback;
                });

        FeedbackResponse response = feedbackService.createFeedback(user.getPublicId(), request);

        assertThat(response.getId()).isEqualTo("10");
        assertThat(response.getCategory()).isEqualTo("feature");
        assertThat(response.getBody()).isEqualTo("퀴즈 결과 화면에서 복습 알림을 받을 수 있으면 좋겠어요.");
        assertThat(response.getReplyEmail()).isEqualTo("user@example.com");
        assertThat(response.getAppVersion()).isEqualTo("1.0.0");
        assertThat(response.getOsVersion()).isEqualTo("Android 15");
        assertThat(response.getDeviceModel()).isEqualTo("Galaxy S24");
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);

        ArgumentCaptor<UserFeedback> feedbackCaptor = ArgumentCaptor.forClass(UserFeedback.class);
        verify(userFeedbackRepository).save(feedbackCaptor.capture());
        UserFeedback savedFeedback = feedbackCaptor.getValue();
        assertThat(savedFeedback.getUserId()).isEqualTo(user.getId());
        assertThat(savedFeedback.getCategory()).isEqualTo("feature");
        assertThat(savedFeedback.getBody()).isEqualTo("퀴즈 결과 화면에서 복습 알림을 받을 수 있으면 좋겠어요.");
        assertThat(savedFeedback.getReplyEmail()).isEqualTo("user@example.com");
        assertThat(savedFeedback.getAppVersion()).isEqualTo("1.0.0");
        assertThat(savedFeedback.getOsVersion()).isEqualTo("Android 15");
        assertThat(savedFeedback.getDeviceModel()).isEqualTo("Galaxy S24");
    }

    @Test
    void createFeedback_throws_not_found_when_user_missing() {
        String userPublicId = "018f8c2e-5f73-7b6a-b9f0-3f55e7f7c901";
        FeedbackCreateRequest request = feedbackCreateRequest();
        when(userRepository.findByPublicIdAndDeletedAtIsNull(userPublicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedbackService.createFeedback(userPublicId, request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_USER_NOT_FOUND);
    }

    private User user() {
        User user = User.create("018f8c2e-5f73-7b6a-b9f0-3f55e7f7c901", "user@example.com", "도토리");
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    private FeedbackCreateRequest feedbackCreateRequest() {
        FeedbackCreateRequest request = new FeedbackCreateRequest();
        ReflectionTestUtils.setField(request, "category", "feature");
        ReflectionTestUtils.setField(request, "body", "퀴즈 결과 화면에서 복습 알림을 받을 수 있으면 좋겠어요.");
        ReflectionTestUtils.setField(request, "replyEmail", "user@example.com");
        ReflectionTestUtils.setField(request, "appVersion", "1.0.0");
        ReflectionTestUtils.setField(request, "osVersion", "Android 15");
        ReflectionTestUtils.setField(request, "deviceModel", "Galaxy S24");
        return request;
    }
}
