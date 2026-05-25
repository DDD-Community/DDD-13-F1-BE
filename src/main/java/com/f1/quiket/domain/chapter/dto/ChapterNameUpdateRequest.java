package com.f1.quiket.domain.chapter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 챕터명 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
public class ChapterNameUpdateRequest {

    /** 챕터명 */
    @NotBlank(message = "챕터명을 입력해주세요")
    @Size(max = 30, message = "챕터명은 30자 이하여야 합니다")
    private String name;
}
