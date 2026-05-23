package com.f1.quiket.domain.home.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 최근활동 페이지 응답 DTO
 */
@Getter
@Builder
public class RecentActivityPageResponse {

    /** 최근활동 목록 */
    private final List<RecentActivityResponse> content;
    /** 페이지 번호 */
    private final int page;
    /** 페이지 크기 */
    private final int size;
    /** 전체 요소 수 */
    private final long totalElements;
    /** 전체 페이지 수 */
    private final int totalPages;
    /** 다음 페이지 존재 여부 */
    private final boolean hasNext;

    /**
     * 페이지 응답 생성
     */
    public static RecentActivityPageResponse of(
            List<RecentActivityResponse> content,
            int page,
            int size,
            long totalElements
    ) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return RecentActivityPageResponse.builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(page + 1 < totalPages)
                .build();
    }
}
