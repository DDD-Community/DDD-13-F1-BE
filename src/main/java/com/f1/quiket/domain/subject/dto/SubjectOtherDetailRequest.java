package com.f1.quiket.domain.subject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 과목 기타 상세 요청 DTO
 */
@Getter
@NoArgsConstructor
public class SubjectOtherDetailRequest {

    /** 이용 목적 */
    @NotBlank(message = "이용 목적은 필수입니다")
    private String usagePurpose;

    /** 추가 설명 */
    @Size(min = 1, max = 100, message = "추가 설명은 1자 이상 100자 이하여야 합니다")
    private String description;
}
