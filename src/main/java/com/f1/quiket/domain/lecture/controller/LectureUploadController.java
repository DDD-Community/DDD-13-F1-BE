package com.f1.quiket.domain.lecture.controller;

import com.f1.quiket.domain.lecture.dto.LectureTextUploadRequest;
import com.f1.quiket.domain.lecture.dto.LectureUploadAcceptedResponse;
import com.f1.quiket.domain.lecture.dto.LectureUploadStatusResponse;
import com.f1.quiket.domain.lecture.service.LectureUploadCreateService;
import com.f1.quiket.domain.lecture.service.LectureUploadStatusService;
import com.f1.quiket.global.auth.UserPrincipal;
import com.f1.quiket.global.response.ApiResponse;
import com.f1.quiket.global.response.SuccessCode;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 강의 업로드 API 진입점
 *
 * PDF, 이미지 OCR, 텍스트 직접 입력 업로드와 처리 상태 조회를 제공
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/lecture-uploads")
public class LectureUploadController {

    private final LectureUploadCreateService lectureUploadCreateService;
    private final LectureUploadStatusService lectureUploadStatusService;

    /**
     * 텍스트 직접 입력 강의 업로드 접수
     *
     * @param principal 인증 사용자
     * @param request 텍스트 업로드 요청
     * @return 업로드 접수 응답
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<LectureUploadAcceptedResponse>> createTextUpload(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody LectureTextUploadRequest request
    ) {
        LectureUploadAcceptedResponse response = lectureUploadCreateService.createTextUpload(
                principal.getUserId(),
                request
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(SuccessCode.ACCEPTED, response));
    }

    /**
     * PDF 또는 이미지 강의 업로드 접수
     *
     * @param principal 인증 사용자
     * @param subjectId 과목 공개 식별자
     * @param chapterName 생성할 챕터명
     * @param uploadType pdf 또는 image
     * @param partSplitMethod auto 또는 manual
     * @param files 업로드 파일 목록
     * @param partSplitPlansJson 직접 분류 계획 JSON 문자열
     * @return 업로드 접수 응답
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<LectureUploadAcceptedResponse>> createFileUpload(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("subjectId") String subjectId,
            @RequestParam("chapterName") String chapterName,
            @RequestParam("uploadType") String uploadType,
            @RequestParam("partSplitMethod") String partSplitMethod,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "partSplitPlansJson", required = false) String partSplitPlansJson
    ) {
        LectureUploadAcceptedResponse response = lectureUploadCreateService.createFileUpload(
                principal.getUserId(),
                subjectId,
                chapterName,
                uploadType,
                partSplitMethod,
                files,
                partSplitPlansJson
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(SuccessCode.ACCEPTED, response));
    }

    /**
     * 강의 업로드 처리 상태 조회
     *
     * @param principal 인증 사용자
     * @param lectureUploadId 업로드 공개 식별자
     * @return 업로드 처리 상태 응답
     */
    @GetMapping("/{lectureUploadId}/status")
    public ResponseEntity<ApiResponse<LectureUploadStatusResponse>> getStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("lectureUploadId") String lectureUploadId
    ) {
        LectureUploadStatusResponse response = lectureUploadStatusService.getStatus(
                principal.getUserId(),
                lectureUploadId
        );
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(SuccessCode.OK, response));
    }
}
