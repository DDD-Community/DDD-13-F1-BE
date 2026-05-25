package com.f1.quiket.domain.subject.dto;

import com.f1.quiket.domain.subject.entity.Subject;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 과목 기본 응답 DTO
 */
@Getter
@Builder
public class SubjectResponse {

    /** 과목 식별자 */
    private final String id;
    /** 과목명 */
    private final String name;
    /** 학습 목적 */
    private final String purpose;
    /** 목적별 상세 */
    private final SubjectPurposeDetailResponse detail;
    /** 생성 시각 */
    private final LocalDateTime createdAt;

    /**
     * 엔티티 응답 변환
     */
    public static SubjectResponse of(Subject subject, SubjectPurposeDetailResponse detail) {
        return SubjectResponse.builder()
                .id(subject.getPublicId())
                .name(subject.getName())
                .purpose(subject.getPurpose())
                .detail(detail)
                .createdAt(subject.getCreatedAt())
                .build();
    }
}
