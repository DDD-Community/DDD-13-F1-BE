package com.f1.quiket.domain.chapter.controller;

import com.f1.quiket.domain.chapter.dto.ChapterNameUpdateRequest;
import com.f1.quiket.domain.chapter.dto.ChapterResponse;
import com.f1.quiket.domain.chapter.service.ChapterService;
import com.f1.quiket.global.auth.UserPrincipal;
import com.f1.quiket.global.response.ApiResponse;
import com.f1.quiket.global.response.SuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 챕터 API 진입점
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/chapters")
public class ChapterController {

    private final ChapterService chapterService;

    /**
     * 챕터명 수정
     */
    @PatchMapping("/{chapterId}/name")
    public ResponseEntity<ApiResponse<ChapterResponse>> updateChapterName(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String chapterId,
            @Valid @RequestBody ChapterNameUpdateRequest request
    ) {
        ChapterResponse response = chapterService.updateChapterName(principal.getPublicId(), chapterId, request.getName());
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }
}
