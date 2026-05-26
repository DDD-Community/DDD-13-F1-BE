package com.f1.quiket.infra.ai.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 공통 바이너리 데이터 DTO
 */
@Getter
@Builder
public class AiBinaryData {
    private final String mimeType;
    private final byte[] bytes;
}

