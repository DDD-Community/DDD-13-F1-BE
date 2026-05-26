package com.f1.quiket.domain.subject.dto;

import com.f1.quiket.domain.subject.entity.SubjectOtherDetail;
import lombok.Builder;
import lombok.Getter;

/**
 * 과목 기타 상세 응답 DTO
 */
@Getter
@Builder
public class SubjectOtherDetailResponse {

    /** 이용 목적 */
    private final String usagePurpose;
    /** 추가 설명 */
    private final String description;

    /**
     * 엔티티 응답 변환
     */
    public static SubjectOtherDetailResponse from(SubjectOtherDetail detail) {
        return SubjectOtherDetailResponse.builder()
                .usagePurpose(detail.getUsagePurpose())
                .description(detail.getDescription())
                .build();
    }
}
