package com.f1.quiket.infra.gemini.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 공통 텍스트 요청 DTO
 */
@Getter
@Builder
public class GeminiCompletionRequest {
    private final String systemMessage;
    private final String userMessage;
}

