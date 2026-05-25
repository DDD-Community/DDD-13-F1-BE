package com.f1.quiket.domain.chapter.dto;

import com.f1.quiket.domain.chapter.entity.Chapter;
import com.f1.quiket.domain.subject.entity.Subject;
import lombok.Builder;
import lombok.Getter;

/**
 * 챕터 응답 DTO
 */
@Getter
@Builder
public class ChapterResponse {

    /** 챕터 식별자 */
    private final String id;
    /** 과목 식별자 */
    private final String subjectId;
    /** 챕터명 */
    private final String name;
    /** 표시 순서 */
    private final Integer displayOrder;

    /**
     * 엔티티 응답 변환
     */
    public static ChapterResponse of(Chapter chapter, Subject subject) {
        return ChapterResponse.builder()
                .id(chapter.getPublicId())
                .subjectId(subject.getPublicId())
                .name(chapter.getName())
                .displayOrder(chapter.getDisplayOrder())
                .build();
    }
}
