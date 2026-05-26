package com.f1.quiket.domain.subject.dto;

import com.f1.quiket.domain.chapter.entity.Chapter;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 챕터와 파트 응답 DTO
 */
@Getter
@Builder
public class ChapterWithPartsResponse {

    /** 챕터 식별자 */
    private final String id;
    /** 과목 식별자 */
    private final String subjectId;
    /** 챕터명 */
    private final String name;
    /** 표시 순서 */
    private final Integer displayOrder;
    /** 파트 목록 */
    private final List<PartSummaryResponse> parts;

    /**
     * 엔티티 응답 변환
     */
    public static ChapterWithPartsResponse of(Chapter chapter, String subjectPublicId, List<PartSummaryResponse> parts) {
        return ChapterWithPartsResponse.builder()
                .id(chapter.getPublicId())
                .subjectId(subjectPublicId)
                .name(chapter.getName())
                .displayOrder(chapter.getDisplayOrder())
                .parts(parts)
                .build();
    }
}
