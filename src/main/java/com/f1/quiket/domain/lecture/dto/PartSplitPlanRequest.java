package com.f1.quiket.domain.lecture.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 직접 분류 파트 계획 요청 DTO
 *
 * 사용자가 지정한 파트 번호와 선택 입력 파트명 전달
 */
@Getter
@NoArgsConstructor
public class PartSplitPlanRequest {

    @Min(value = 1, message = "partNumber는 1 이상이어야 합니다.")
    private Integer partNumber;

    @Size(max = 30, message = "파트명은 30자 이하로 입력해주세요.")
    private String intendedName;
}
