package com.f1.quiket.domain.subject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 과목명 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
public class SubjectNameUpdateRequest {

    /** 과목명 */
    @NotBlank(message = "과목명을 입력해주세요")
    @Size(max = 30, message = "과목명은 30자 이하여야 합니다")
    private String name;
}
