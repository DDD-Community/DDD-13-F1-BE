package com.f1.quiket.domain.subject.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 과목 페이지 응답 DTO
 */
@Getter
@Builder
public class SubjectPageResponse {

    /** 현재 페이지 번호 */
    private final Integer page;
    /** 페이지 크기 */
    private final Integer size;
    /** 전체 건수 */
    private final Long totalElements;
    /** 전체 페이지 수 */
    private final Integer totalPages;
    /** 마지막 페이지 여부 */
    private final Boolean last;
    /** 과목 목록 */
    private final List<SubjectSummaryResponse> content;
}
