package com.f1.quiket.domain.part.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 파트 수정 요청 DTO
 *
 * 변경할 파트명과 본문 전달
 */
@Getter
@NoArgsConstructor
public class PartUpdateRequest {

    private String name;
    private String content;
}
