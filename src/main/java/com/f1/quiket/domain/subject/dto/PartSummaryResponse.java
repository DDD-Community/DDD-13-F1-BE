package com.f1.quiket.domain.subject.dto;

import com.f1.quiket.domain.chapter.entity.Chapter;
import com.f1.quiket.domain.part.entity.Part;
import lombok.Builder;
import lombok.Getter;

/**
 * 파트 요약 응답 DTO
 */
@Getter
@Builder
public class PartSummaryResponse {

    /** 파트 식별자 */
    private final String id;
    /** 챕터 식별자 */
    private final String chapterId;
    /** 파트명 */
    private final String name;
    /** 파트 번호 */
    private final Integer partNumber;

    /**
     * 엔티티 응답 변환
     */
    public static PartSummaryResponse of(Part part, Chapter chapter) {
        return PartSummaryResponse.builder()
                .id(part.getPublicId())
                .chapterId(chapter.getPublicId())
                .name(part.getName())
                .partNumber(part.getPartNumber())
                .build();
    }
}
