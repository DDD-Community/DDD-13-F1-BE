package com.f1.quiket.domain.subject.dto;

import com.f1.quiket.domain.subject.entity.SubjectReviewDetail;
import lombok.Builder;
import lombok.Getter;

/**
 * 과목 복습 상세 응답 DTO
 */
@Getter
@Builder
public class SubjectReviewDetailResponse {

    /** 학습 분야 */
    private final String field;
    /** 학습 정도 */
    private final String studyLevel;

    /**
     * 엔티티 응답 변환
     */
    public static SubjectReviewDetailResponse from(SubjectReviewDetail detail) {
        return SubjectReviewDetailResponse.builder()
                .field(detail.getField())
                .studyLevel(detail.getStudyLevel())
                .build();
    }
}
