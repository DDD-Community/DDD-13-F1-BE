package com.f1.quiket.infra.gemini.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Gemini 콘텐츠 생성 응답 DTO
 *
 * 응답 텍스트 전달
 */
@Getter
@Builder
public class GeminiCompletionResponse {
    private final String content;
}

