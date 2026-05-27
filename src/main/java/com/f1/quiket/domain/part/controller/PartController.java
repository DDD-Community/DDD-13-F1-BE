package com.f1.quiket.domain.part.controller;

import com.f1.quiket.domain.lecture.dto.LectureUploadAcceptedResponse;
import com.f1.quiket.domain.part.dto.PartResponse;
import com.f1.quiket.domain.part.dto.PartTextAddRequest;
import com.f1.quiket.domain.part.dto.PartUpdateRequest;
import com.f1.quiket.domain.part.service.PartAddCreateService;
import com.f1.quiket.domain.part.service.PartQueryService;
import com.f1.quiket.global.auth.UserPrincipal;
import com.f1.quiket.global.response.ApiResponse;
import com.f1.quiket.global.response.SuccessCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 파트 관리 API 진입점
 *
 * 파트 상세 조회, 파트명/본문 수정, 기존 챕터 파트 추가를 제공
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class PartController {

    private final PartQueryService partQueryService;
    private final PartAddCreateService partAddCreateService;

    /**
     * 파트 상세 조회
     *
     * @param principal 인증 사용자
     * @param partId 파트 공개 식별자
     * @return 파트 상세 응답
     */
    @GetMapping("/parts/{partId}")
    public ResponseEntity<ApiResponse<PartResponse>> getPart(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("partId") String partId
    ) {
        PartResponse response = partQueryService.getPart(principal.getUserId(), partId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(SuccessCode.OK, response));
    }

    /**
     * 파트명 및 본문 수정
     *
     * @param principal 인증 사용자
     * @param partId 파트 공개 식별자
     * @param request 파트 수정 요청
     * @return 수정된 파트 상세 응답
     */
    @PatchMapping("/parts/{partId}")
    public ResponseEntity<ApiResponse<PartResponse>> updatePart(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("partId") String partId,
            @RequestBody PartUpdateRequest request
    ) {
        PartResponse response = partQueryService.updatePart(principal.getUserId(), partId, request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(SuccessCode.OK, response));
    }

    /**
     * 텍스트 직접 입력 파트 추가 접수
     *
     * @param principal 인증 사용자
     * @param chapterId 챕터 공개 식별자
     * @param request 텍스트 파트 추가 요청
     * @return 업로드 접수 응답
     */
    @PostMapping(value = "/chapters/{chapterId}/parts", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<LectureUploadAcceptedResponse>> addTextPart(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("chapterId") String chapterId,
            @RequestBody PartTextAddRequest request
    ) {
        LectureUploadAcceptedResponse response = partAddCreateService.createTextPart(
                principal.getUserId(),
                chapterId,
                request
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(SuccessCode.ACCEPTED, response));
    }

    /**
     * PDF 또는 이미지 파일 파트 추가 접수
     *
     * @param principal 인증 사용자
     * @param chapterId 챕터 공개 식별자
     * @param partName 파트명
     * @param uploadType pdf 또는 image
     * @param files 업로드 파일 목록
     * @return 업로드 접수 응답
     */
    @PostMapping(value = "/chapters/{chapterId}/parts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<LectureUploadAcceptedResponse>> addFilePart(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("chapterId") String chapterId,
            @RequestParam("partName") String partName,
            @RequestParam("uploadType") String uploadType,
            @RequestParam("files") List<MultipartFile> files
    ) {
        LectureUploadAcceptedResponse response = partAddCreateService.createFilePart(
                principal.getUserId(),
                chapterId,
                partName,
                uploadType,
                files
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(SuccessCode.ACCEPTED, response));
    }
}
