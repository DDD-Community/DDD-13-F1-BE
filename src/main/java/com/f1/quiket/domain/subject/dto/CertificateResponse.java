package com.f1.quiket.domain.subject.dto;

import com.f1.quiket.domain.subject.entity.Certificate;
import lombok.Builder;
import lombok.Getter;

/**
 * 자격증 응답 DTO
 */
@Getter
@Builder
public class CertificateResponse {

    /** 자격증 식별자 */
    private final Long id;
    /** 자격증명 */
    private final String name;
    /** 자주 찾는 자격증 여부 */
    private final Boolean featured;
    /** 표시 순서 */
    private final Integer displayOrder;

    /**
     * 엔티티 응답 변환
     */
    public static CertificateResponse from(Certificate certificate) {
        return CertificateResponse.builder()
                .id(certificate.getId())
                .name(certificate.getName())
                .featured(certificate.getFeatured())
                .displayOrder(certificate.getDisplayOrder())
                .build();
    }
}
