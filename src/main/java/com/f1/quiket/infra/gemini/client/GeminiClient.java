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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
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

    public GeminiCompletionResponse generate(GeminiCompletionRequest request, List<GeminiBinaryData> binaryData) {
        validateConfigured();
        try {
            String responseBody = restClient.post()
                    .uri(generateUri())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(buildRequestBody(request, binaryData))
                    .retrieve()
                    .body(String.class);
            return GeminiCompletionResponse.builder().content(parseResponse(responseBody)).build();
        } catch (CustomException e) {
            throw e;
        } catch (RestClientException | JsonProcessingException e) {
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, "Gemini API 호출에 실패했습니다.", e);
        }
    }

    private void validateConfigured() {
        if (!properties.isConfigured()) {
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, "Gemini 설정값이 준비되지 않았습니다.");
        }
    }

    private String generateUri() {
        String baseUrl = properties.getBaseUrl().replaceAll("/+$", "");
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path("/v1beta/models/" + properties.getModel() + ":generateContent")
                .queryParam("key", properties.getApiKey())
                .build()
                .toUriString();
    }

    private Map<String, Object> buildRequestBody(GeminiCompletionRequest request, List<GeminiBinaryData> binaryData) {
        List<Map<String, Object>> parts = new ArrayList<>();
        if (StringUtils.hasText(request.getSystemMessage())) {
            parts.add(Map.of("text", request.getSystemMessage()));
        }
        parts.add(Map.of("text", request.getUserMessage()));

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

    private String parseResponse(String responseBody) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (textNode.isMissingNode() || textNode.isNull() || !StringUtils.hasText(textNode.asText())) {
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, "Gemini 응답 본문이 비어 있습니다.");
        }
        return textNode.asText();
    }
}
