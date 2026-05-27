package com.f1.quiket.infra.gemini.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 공통 바이너리 데이터 DTO
 */
@Getter
@Builder
public class GeminiBinaryData {
    private final String mimeType;
    private final byte[] bytes;
}

