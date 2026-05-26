package com.f1.quiket.domain.chapter.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.chapter.dto.ChapterNameUpdateRequest;
import com.f1.quiket.domain.chapter.dto.ChapterResponse;
import com.f1.quiket.domain.chapter.service.ChapterService;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.global.auth.UserPrincipal;
import com.f1.quiket.global.response.ApiResponse;
import com.f1.quiket.global.response.SuccessCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

class ChapterControllerTest {

    private ChapterService chapterService;
    private ChapterController chapterController;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        chapterService = mock(ChapterService.class);
        chapterController = new ChapterController(chapterService);
        principal = principal("user-public-id");
    }

    @Test
    void updateChapterName_returns_ok_response() {
        ChapterNameUpdateRequest request = new ChapterNameUpdateRequest();
        ReflectionTestUtils.setField(request, "name", "트랜잭션");

        ChapterResponse response = ChapterResponse.builder()
                .id("chapter-public-id")
                .subjectId("subject-public-id")
                .name("트랜잭션")
                .displayOrder(1)
                .build();
        when(chapterService.updateChapterName(principal.getPublicId(), "chapter-public-id", "트랜잭션")).thenReturn(response);

        ResponseEntity<ApiResponse<ChapterResponse>> result = chapterController.updateChapterName(principal, "chapter-public-id", request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().isSuccess()).isTrue();
        assertThat(result.getBody().getCode()).isEqualTo(SuccessCode.OK.getCode());
        assertThat(result.getBody().getData()).isSameAs(response);
        verify(chapterService).updateChapterName(principal.getPublicId(), "chapter-public-id", "트랜잭션");
    }

    private UserPrincipal principal(String userPublicId) {
        User user = User.create(userPublicId, "user@example.com", "도토리");
        ReflectionTestUtils.setField(user, "id", 1L);
        return UserPrincipal.from(user);
    }
}
