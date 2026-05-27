package com.f1.quiket.domain.part.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 텍스트 직접 입력 파트 추가 요청 DTO
 *
 * 추가할 파트명, 업로드 타입, 본문 텍스트 전달
 */
@Getter
@NoArgsConstructor
public class PartTextAddRequest {

    private String partName;
    private String uploadType;
    private String text;
}
