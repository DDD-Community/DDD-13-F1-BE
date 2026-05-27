package com.f1.quiket.infra.gemini.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Gemini 바이너리 데이터 DTO
 *
 * inlineData 요청에 사용할 MIME 타입과 원본 바이트 전달
 */
@Getter
@Builder
public class GeminiBinaryData {
    private final String mimeType;
    private final byte[] bytes;
}

