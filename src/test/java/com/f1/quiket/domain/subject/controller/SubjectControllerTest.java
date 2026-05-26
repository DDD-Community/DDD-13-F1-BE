package com.f1.quiket.domain.subject.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.subject.dto.QuizScopeResponse;
import com.f1.quiket.domain.subject.dto.SubjectCreateRequest;
import com.f1.quiket.domain.subject.dto.SubjectDetailResponse;
import com.f1.quiket.domain.subject.dto.SubjectDetailUpdateRequest;
import com.f1.quiket.domain.subject.dto.SubjectExamScheduleResponse;
import com.f1.quiket.domain.subject.dto.SubjectExamScheduleUpsertRequest;
import com.f1.quiket.domain.subject.dto.SubjectNameUpdateRequest;
import com.f1.quiket.domain.subject.dto.SubjectPageResponse;
import com.f1.quiket.domain.subject.dto.SubjectResponse;
import com.f1.quiket.domain.subject.dto.SubjectSummaryResponse;
import com.f1.quiket.domain.subject.service.SubjectService;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.global.auth.UserPrincipal;
import com.f1.quiket.global.response.ApiResponse;
import com.f1.quiket.global.response.SuccessCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

class SubjectControllerTest {

    private SubjectService subjectService;
    private SubjectController subjectController;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        subjectService = mock(SubjectService.class);
        subjectController = new SubjectController(subjectService);
        principal = principal("user-public-id");
    }

    @Test
    void getSubjects_returns_ok_response() {
        SubjectPageResponse response = SubjectPageResponse.builder()
                .page(0)
                .size(10)
                .totalElements(1L)
                .totalPages(1)
                .last(true)
                .content(List.of(SubjectSummaryResponse.builder()
                        .id("subject-public-id")
                        .name("데이터베이스")
                        .purpose("exam")
                        .build()))
                .build();
        when(subjectService.getSubjects(principal.getPublicId(), 0, 10)).thenReturn(response);

        ResponseEntity<ApiResponse<SubjectPageResponse>> result = subjectController.getSubjects(principal, 0, 10);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().isSuccess()).isTrue();
        assertThat(result.getBody().getCode()).isEqualTo(SuccessCode.OK.getCode());
        assertThat(result.getBody().getData()).isSameAs(response);
        verify(subjectService).getSubjects(principal.getPublicId(), 0, 10);
    }

    @Test
    void createSubject_returns_created_response() {
        SubjectCreateRequest request = new SubjectCreateRequest();
        ReflectionTestUtils.setField(request, "name", "데이터베이스");
        ReflectionTestUtils.setField(request, "purpose", "exam");

        SubjectResponse response = SubjectResponse.builder()
                .id("subject-public-id")
                .name("데이터베이스")
                .purpose("exam")
                .createdAt(LocalDateTime.now())
                .build();
        when(subjectService.createSubject(principal.getPublicId(), request)).thenReturn(response);

        ResponseEntity<ApiResponse<SubjectResponse>> result = subjectController.createSubject(principal, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().isSuccess()).isTrue();
        assertThat(result.getBody().getCode()).isEqualTo(SuccessCode.CREATED.getCode());
        assertThat(result.getBody().getData()).isSameAs(response);
        verify(subjectService).createSubject(principal.getPublicId(), request);
    }

    @Test
    void getSubject_returns_ok_response() {
        SubjectDetailResponse response = SubjectDetailResponse.builder()
                .id("subject-public-id")
                .name("데이터베이스")
                .purpose("exam")
                .build();
        when(subjectService.getSubject(principal.getPublicId(), "subject-public-id")).thenReturn(response);

        ResponseEntity<ApiResponse<SubjectDetailResponse>> result = subjectController.getSubject(principal, "subject-public-id");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().isSuccess()).isTrue();
        assertThat(result.getBody().getCode()).isEqualTo(SuccessCode.OK.getCode());
        assertThat(result.getBody().getData()).isSameAs(response);
        verify(subjectService).getSubject(principal.getPublicId(), "subject-public-id");
    }

    @Test
    void getQuizScope_returns_ok_response() {
        QuizScopeResponse response = QuizScopeResponse.builder()
                .subjectId("subject-public-id")
                .subjectName("데이터베이스")
                .chapters(List.of())
                .build();
        when(subjectService.getQuizScope(principal.getPublicId(), "subject-public-id")).thenReturn(response);

        ResponseEntity<ApiResponse<QuizScopeResponse>> result = subjectController.getQuizScope(principal, "subject-public-id");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().isSuccess()).isTrue();
        assertThat(result.getBody().getCode()).isEqualTo(SuccessCode.OK.getCode());
        assertThat(result.getBody().getData()).isSameAs(response);
        verify(subjectService).getQuizScope(principal.getPublicId(), "subject-public-id");
    }

    @Test
    void deleteSubject_returns_ok_response() {
        ResponseEntity<ApiResponse<Void>> result = subjectController.deleteSubject(principal, "subject-public-id");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().isSuccess()).isTrue();
        assertThat(result.getBody().getCode()).isEqualTo(SuccessCode.OK.getCode());
        assertThat(result.getBody().getData()).isNull();
        verify(subjectService).deleteSubject(principal.getPublicId(), "subject-public-id");
    }

    @Test
    void updateSubjectName_returns_ok_response() {
        SubjectNameUpdateRequest request = new SubjectNameUpdateRequest();
        ReflectionTestUtils.setField(request, "name", "운영체제");

        SubjectResponse response = SubjectResponse.builder()
                .id("subject-public-id")
                .name("운영체제")
                .purpose("exam")
                .build();
        when(subjectService.updateSubjectName(principal.getPublicId(), "subject-public-id", "운영체제")).thenReturn(response);

        ResponseEntity<ApiResponse<SubjectResponse>> result = subjectController.updateSubjectName(principal, "subject-public-id", request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().isSuccess()).isTrue();
        assertThat(result.getBody().getCode()).isEqualTo(SuccessCode.OK.getCode());
        assertThat(result.getBody().getData()).isSameAs(response);
        verify(subjectService).updateSubjectName(principal.getPublicId(), "subject-public-id", "운영체제");
    }

    @Test
    void updateSubjectDetails_returns_ok_response() {
        SubjectDetailUpdateRequest request = new SubjectDetailUpdateRequest();
        ReflectionTestUtils.setField(request, "name", "운영체제");
        ReflectionTestUtils.setField(request, "purpose", "review");

        SubjectDetailResponse response = SubjectDetailResponse.builder()
                .id("subject-public-id")
                .name("운영체제")
                .purpose("review")
                .build();
        when(subjectService.updateSubjectDetails(principal.getPublicId(), "subject-public-id", request)).thenReturn(response);

        ResponseEntity<ApiResponse<SubjectDetailResponse>> result = subjectController.updateSubjectDetails(principal, "subject-public-id", request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().isSuccess()).isTrue();
        assertThat(result.getBody().getCode()).isEqualTo(SuccessCode.OK.getCode());
        assertThat(result.getBody().getData()).isSameAs(response);
        verify(subjectService).updateSubjectDetails(principal.getPublicId(), "subject-public-id", request);
    }

    @Test
    void upsertSubjectExamSchedule_returns_ok_response() {
        SubjectExamScheduleUpsertRequest request = new SubjectExamScheduleUpsertRequest();
        ReflectionTestUtils.setField(request, "examName", "정보처리기사");
        ReflectionTestUtils.setField(request, "examDate", LocalDate.now().plusDays(30));

        SubjectExamScheduleResponse response = SubjectExamScheduleResponse.builder()
                .id("schedule-public-id")
                .subjectId("subject-public-id")
                .examName("정보처리기사")
                .examDate(LocalDate.now().plusDays(30))
                .dDay(30)
                .build();
        when(subjectService.upsertExamSchedule(principal.getPublicId(), "subject-public-id", request)).thenReturn(response);

        ResponseEntity<ApiResponse<SubjectExamScheduleResponse>> result = subjectController.upsertSubjectExamSchedule(principal, "subject-public-id", request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().isSuccess()).isTrue();
        assertThat(result.getBody().getCode()).isEqualTo(SuccessCode.OK.getCode());
        assertThat(result.getBody().getData()).isSameAs(response);
        verify(subjectService).upsertExamSchedule(principal.getPublicId(), "subject-public-id", request);
    }

    @Test
    void deleteSubjectExamSchedule_returns_ok_response() {
        ResponseEntity<ApiResponse<Void>> result = subjectController.deleteSubjectExamSchedule(principal, "subject-public-id");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().isSuccess()).isTrue();
        assertThat(result.getBody().getCode()).isEqualTo(SuccessCode.OK.getCode());
        assertThat(result.getBody().getData()).isNull();
        verify(subjectService).deleteExamSchedule(principal.getPublicId(), "subject-public-id");
    }

    private UserPrincipal principal(String userPublicId) {
        User user = User.create(userPublicId, "user@example.com", "도토리");
        ReflectionTestUtils.setField(user, "id", 1L);
        return UserPrincipal.from(user);
    }
}
