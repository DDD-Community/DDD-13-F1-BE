package com.f1.quiket.infra.groq.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Groq 텍스트 응답 DTO
 *
 * 응답 텍스트 전달
 */
@Getter
@Builder
public class GroqCompletionResponse {
    private final String content;
}
