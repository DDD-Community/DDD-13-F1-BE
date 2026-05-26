package com.f1.quiket.infra.ai.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 공통 텍스트 응답 DTO
 */
@Getter
@Builder
public class AiCompletionResponse {
    private final String content;
}

