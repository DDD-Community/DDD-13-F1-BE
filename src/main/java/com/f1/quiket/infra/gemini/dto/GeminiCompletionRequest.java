package com.f1.quiket.infra.gemini.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Gemini 콘텐츠 생성 요청 DTO
 *
 * 시스템 메시지와 사용자 메시지 전달
 */
@Getter
@Builder
public class GeminiCompletionRequest {
    private final String systemMessage;
    private final String userMessage;
}

