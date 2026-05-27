package com.f1.quiket.infra.gemini.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import com.f1.quiket.infra.gemini.config.GeminiProperties;
import com.f1.quiket.infra.gemini.dto.GeminiBinaryData;
import com.f1.quiket.infra.gemini.dto.GeminiCompletionRequest;
import com.f1.quiket.infra.gemini.dto.GeminiCompletionResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Gemini generateContent API 클라이언트
 *
 * 이미지와 PDF 기반 OCR 요청 처리
 */
@Component
@Slf4j
public class GeminiClient {

    private final RestClient restClient;
    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;

    public GeminiClient(
            @Qualifier("geminiRestClient") RestClient restClient,
            GeminiProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Gemini 콘텐츠 생성 요청
     */
    public GeminiCompletionResponse generate(GeminiCompletionRequest request, List<GeminiBinaryData> binaryData) {
        validateConfigured();
        try {
            Map<String, Object> requestBody = buildRequestBody(request, binaryData);
            String uri = generateUri();
            log.debug("Gemini API request uri={}, body={}", maskApiKey(uri), toJsonForLog(requestBody));

            // Gemini generateContent 호출
            String responseBody = restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
            log.debug("Gemini API response body={}", responseBody);
            return GeminiCompletionResponse.builder().content(parseResponse(responseBody)).build();
        } catch (CustomException e) {
            throw e;
        } catch (RestClientException | JsonProcessingException e) {
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, "Gemini API 호출에 실패했습니다.", e);
        }
    }

    /**
     * Gemini 설정값 검증
     */
    private void validateConfigured() {
        if (!properties.isConfigured()) {
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, "Gemini 설정값이 준비되지 않았습니다.");
        }
    }

    /**
     * Gemini generateContent URI 생성
     */
    private String generateUri() {
        String baseUrl = properties.getBaseUrl().replaceAll("/+$", "");
        String path = properties.getGenerateContentPath().replace("{model}", properties.getModel());
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path(path)
                .queryParam("key", properties.getApiKey())
                .build()
                .toUriString();
    }

    /**
     * Gemini 요청 본문 생성
     */
    private Map<String, Object> buildRequestBody(GeminiCompletionRequest request, List<GeminiBinaryData> binaryData) {
        List<Map<String, Object>> parts = new ArrayList<>();
        if (StringUtils.hasText(request.getSystemMessage())) {
            parts.add(Map.of("text", request.getSystemMessage()));
        }
        parts.add(Map.of("text", request.getUserMessage()));

        // 이미지/PDF 바이너리 inlineData 변환
        if (binaryData != null) {
            for (GeminiBinaryData file : binaryData) {
                parts.add(Map.of(
                        "inlineData", Map.of(
                                "mimeType", file.getMimeType(),
                                "data", Base64.getEncoder().encodeToString(file.getBytes())
                        )
                ));
            }
        }

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", properties.getTemperature());
        generationConfig.put("responseMimeType", "application/json");

        return Map.of(
                "contents", List.of(Map.of("role", "user", "parts", parts)),
                "generationConfig", generationConfig
        );
    }

    /**
     * Gemini 응답 텍스트 파싱
     */
    private String parseResponse(String responseBody) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (textNode.isMissingNode() || textNode.isNull() || !StringUtils.hasText(textNode.asText())) {
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, "Gemini 응답 본문이 비어 있습니다.");
        }
        return textNode.asText();
    }

    private String toJsonForLog(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }

    private String maskApiKey(String uri) {
        return uri.replaceAll("([?&]key=)[^&]+", "$1***");
    }
}
