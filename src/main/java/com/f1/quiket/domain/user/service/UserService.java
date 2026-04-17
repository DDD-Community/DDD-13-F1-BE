package com.f1.quiket.domain.user.service;

import com.f1.quiket.domain.user.dto.UserResponse;
import com.f1.quiket.domain.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    /**
     * 활성화된 사용자 목록 조회.
     */
    public List<UserResponse> findActiveUsers() {
        return userRepository.findAllByStatusAndDeletedAtIsNull("active")
                .stream()
                .map(UserResponse::from)
                .toList();
    }
}
