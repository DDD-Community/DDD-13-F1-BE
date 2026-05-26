package com.f1.quiket.infra.ai.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 공통 텍스트 요청 DTO
 */
@Getter
@Builder
public class AiCompletionRequest {
    private final String systemMessage;
    private final String userMessage;
}

