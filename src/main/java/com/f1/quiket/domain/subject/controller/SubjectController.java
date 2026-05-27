package com.f1.quiket.domain.subject.controller;

import com.f1.quiket.domain.quiz.dto.QuizScopeResponse;
import com.f1.quiket.domain.quiz.service.QuizScopeService;
import com.f1.quiket.domain.subject.dto.SubjectCreateRequest;
import com.f1.quiket.domain.subject.dto.SubjectDetailResponse;
import com.f1.quiket.domain.subject.dto.SubjectDetailUpdateRequest;
import com.f1.quiket.domain.subject.dto.SubjectExamScheduleResponse;
import com.f1.quiket.domain.subject.dto.SubjectExamScheduleUpsertRequest;
import com.f1.quiket.domain.subject.dto.SubjectNameUpdateRequest;
import com.f1.quiket.domain.subject.dto.SubjectPageResponse;
import com.f1.quiket.domain.subject.dto.SubjectResponse;
import com.f1.quiket.domain.subject.service.SubjectService;
import com.f1.quiket.global.auth.UserPrincipal;
import com.f1.quiket.global.response.ApiResponse;
import com.f1.quiket.global.response.SuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 과목 API 진입점
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/subjects")
public class SubjectController {

    private final SubjectService subjectService;
    private final QuizScopeService quizScopeService;

    /**
     * 내 과목 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<SubjectPageResponse>> getSubjects(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        SubjectPageResponse response = subjectService.getSubjects(principal.getPublicId(), page, size);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    /**
     * 과목 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SubjectResponse>> createSubject(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SubjectCreateRequest request
    ) {
        SubjectResponse response = subjectService.createSubject(principal.getPublicId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(SuccessCode.CREATED, response));
    }

    /**
     * 과목 상세 조회
     */
    @GetMapping("/{subjectId}")
    public ResponseEntity<ApiResponse<SubjectDetailResponse>> getSubject(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String subjectId
    ) {
        SubjectDetailResponse response = subjectService.getSubject(principal.getPublicId(), subjectId);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    /**
     * 퀴즈 출제 범위 조회
     */
    @GetMapping("/{subjectId}/quiz-scope")
    public ResponseEntity<ApiResponse<QuizScopeResponse>> getQuizScope(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String subjectId
    ) {
        QuizScopeResponse response = quizScopeService.getQuizScope(principal.getUserId(), subjectId);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    /**
     * 과목 삭제
     */
    @DeleteMapping("/{subjectId}")
    public ResponseEntity<ApiResponse<Void>> deleteSubject(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String subjectId
    ) {
        subjectService.deleteSubject(principal.getPublicId(), subjectId);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK));
    }

    /**
     * 과목명 수정
     */
    @PatchMapping("/{subjectId}/name")
    public ResponseEntity<ApiResponse<SubjectResponse>> updateSubjectName(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String subjectId,
            @Valid @RequestBody SubjectNameUpdateRequest request
    ) {
        SubjectResponse response = subjectService.updateSubjectName(principal.getPublicId(), subjectId, request.getName());
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    /**
     * 과목 상세 수정
     */
    @PutMapping("/{subjectId}/details")
    public ResponseEntity<ApiResponse<SubjectDetailResponse>> updateSubjectDetails(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String subjectId,
            @Valid @RequestBody SubjectDetailUpdateRequest request
    ) {
        SubjectDetailResponse response = subjectService.updateSubjectDetails(principal.getPublicId(), subjectId, request);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    /**
     * 시험 일정 등록 또는 수정
     */
    @PutMapping("/{subjectId}/exam-schedule")
    public ResponseEntity<ApiResponse<SubjectExamScheduleResponse>> upsertSubjectExamSchedule(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String subjectId,
            @Valid @RequestBody SubjectExamScheduleUpsertRequest request
    ) {
        SubjectExamScheduleResponse response = subjectService.upsertExamSchedule(principal.getPublicId(), subjectId, request);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    /**
     * 시험 일정 삭제
     */
    @DeleteMapping("/{subjectId}/exam-schedule")
    public ResponseEntity<ApiResponse<Void>> deleteSubjectExamSchedule(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String subjectId
    ) {
        subjectService.deleteExamSchedule(principal.getPublicId(), subjectId);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK));
    }
}
