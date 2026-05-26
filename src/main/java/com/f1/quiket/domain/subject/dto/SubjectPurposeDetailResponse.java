package com.f1.quiket.domain.subject.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 과목 목적별 상세 응답 DTO
 */
@Getter
@Builder
public class SubjectPurposeDetailResponse {

    /** 시험 상세 */
    private final SubjectExamDetailResponse examDetail;
    /** 복습 상세 */
    private final SubjectReviewDetailResponse reviewDetail;
    /** 기타 상세 */
    private final SubjectOtherDetailResponse otherDetail;
}
