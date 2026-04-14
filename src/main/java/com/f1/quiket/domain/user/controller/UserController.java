package com.f1.quiket.domain.user.controller;

import com.f1.quiket.domain.user.dto.UserResponse;
import com.f1.quiket.domain.user.service.UserService;
import com.f1.quiket.global.response.ApiResponse;
import com.f1.quiket.global.response.SuccessCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 관련 API 진입점
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 활성화된 사용자 목록 조회.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsers() {
        List<UserResponse> users = userService.findActiveUsers();
        return ResponseEntity.ok(ApiResponse.of(SuccessCode.OK, users));
    }

}
