package com.f1.quiket.infra.groq.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Groq 텍스트 요청 DTO
 *
 * 시스템 메시지와 사용자 메시지 전달
 */
@Getter
@Builder
public class GroqCompletionRequest {
    private final String systemMessage;
    private final String userMessage;
}
