package com.f1.quiket.domain.subject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 과목 복습 상세 요청 DTO
 */
@Getter
@NoArgsConstructor
public class SubjectReviewDetailRequest {

    /** 학습 분야 */
    @NotBlank(message = "분야는 필수입니다")
    @Size(max = 30, message = "분야는 30자 이하여야 합니다")
    private String field;

    /** 학습 정도 */
    @NotBlank(message = "학습 정도는 필수입니다")
    private String studyLevel;
}
