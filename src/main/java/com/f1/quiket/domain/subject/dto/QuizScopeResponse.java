package com.f1.quiket.domain.subject.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 퀴즈 출제 범위 응답 DTO
 */
@Getter
@Builder
public class QuizScopeResponse {

    /** 과목 식별자 */
    private final String subjectId;
    /** 과목명 */
    private final String subjectName;
    /** 챕터 목록 */
    private final List<ChapterWithPartsResponse> chapters;
}
