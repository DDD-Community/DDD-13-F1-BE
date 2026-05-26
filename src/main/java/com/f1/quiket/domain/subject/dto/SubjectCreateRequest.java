package com.f1.quiket.domain.subject.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 과목 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
public class SubjectCreateRequest {

    /** 과목명 */
    @NotBlank(message = "과목명을 입력해주세요")
    @Size(max = 30, message = "과목명은 30자 이하여야 합니다")
    private String name;

    /** 학습 목적 */
    @NotBlank(message = "학습 목적은 필수입니다")
    private String purpose;

    /** 시험 상세 */
    @Valid
    private SubjectExamDetailRequest examDetail;

    /** 복습 상세 */
    @Valid
    private SubjectReviewDetailRequest reviewDetail;

    /** 기타 상세 */
    @Valid
    private SubjectOtherDetailRequest otherDetail;
}
