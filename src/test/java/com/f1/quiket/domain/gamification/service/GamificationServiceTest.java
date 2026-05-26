package com.f1.quiket.domain.gamification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.gamification.dto.GamificationResponse;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.domain.user.repository.UserRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class GamificationServiceTest {

    private UserRepository userRepository;
    private GamificationService gamificationService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        gamificationService = new GamificationService(userRepository);
    }

    @Test
    void getMyGamification_returns_level_progress() {
        User user = user(20, 360, 3);
        when(userRepository.findByPublicIdAndDeletedAtIsNull(user.getPublicId()))
                .thenReturn(Optional.of(user));

        GamificationResponse response = gamificationService.getMyGamification(user.getPublicId());

        assertThat(response.getDotoriBalance()).isEqualTo(20);
        assertThat(response.getXpTotal()).isEqualTo(360);
        assertThat(response.getCurrentLevel()).isEqualTo(3);
        assertThat(response.getCurrentLevelName()).isEqualTo("펜굴리는 다람쥐");
        assertThat(response.getMaxLevel()).isEqualTo(10);
        assertThat(response.getNextLevel()).isEqualTo(4);
        assertThat(response.getNextLevelName()).isEqualTo("노력형 다람쥐");
        assertThat(response.getCurrentLevelMinXp()).isEqualTo(350);
        assertThat(response.getNextLevelRequiredXp()).isEqualTo(850);
        assertThat(response.getLevelProgressPct()).isEqualTo(2);
    }

    @Test
    void getMyGamification_returns_max_level_without_next_level() {
        User user = user(99, 30000, 10);
        when(userRepository.findByPublicIdAndDeletedAtIsNull(user.getPublicId()))
                .thenReturn(Optional.of(user));

        GamificationResponse response = gamificationService.getMyGamification(user.getPublicId());

        assertThat(response.getCurrentLevel()).isEqualTo(10);
        assertThat(response.getNextLevel()).isNull();
        assertThat(response.getNextLevelName()).isNull();
        assertThat(response.getNextLevelRequiredXp()).isNull();
        assertThat(response.getLevelProgressPct()).isEqualTo(100);
    }

    @Test
    void getMyGamification_throws_not_found_when_user_missing() {
        when(userRepository.findByPublicIdAndDeletedAtIsNull("missing-user"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> gamificationService.getMyGamification("missing-user"))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_USER_NOT_FOUND);
    }

    private User user(Integer dotoriBalance, Integer xpTotal, Integer currentLevel) {
        User user = User.create("user-public-id", "user@example.com", "도토리");
        ReflectionTestUtils.setField(user, "dotoriBalance", dotoriBalance);
        ReflectionTestUtils.setField(user, "xpTotal", xpTotal);
        ReflectionTestUtils.setField(user, "currentLevel", currentLevel);
        return user;
    }
}
