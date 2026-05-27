package com.f1.quiket.infra.groq.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import com.f1.quiket.infra.groq.config.GroqProperties;
import com.f1.quiket.infra.groq.dto.GroqCompletionRequest;
import com.f1.quiket.infra.groq.dto.GroqCompletionResponse;
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
public class GroqClient {

    private final RestClient restClient;
    private final GroqProperties properties;
    private final ObjectMapper objectMapper;

    public GroqClient(
            @Qualifier("groqRestClient") RestClient restClient,
            GroqProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
    }

    public GroqCompletionResponse generate(GroqCompletionRequest request) {
        validateConfigured();
        try {
            String responseBody = restClient.post()
                    .uri(chatCompletionUri())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .body(requestBody(request))
                    .retrieve()
                    .body(String.class);
            return GroqCompletionResponse.builder().content(parseResponse(responseBody)).build();
        } catch (CustomException e) {
            throw e;
        } catch (RestClientException | JsonProcessingException e) {
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, "Groq API 호출에 실패했습니다.", e);
        }
    }

    private void validateConfigured() {
        if (!properties.isConfigured()) {
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, "Groq 설정값이 준비되지 않았습니다.");
        }
    }

    private String chatCompletionUri() {
        String baseUrl = properties.getBaseUrl().replaceAll("/+$", "");
        return UriComponentsBuilder.fromUriString(baseUrl)
                .pathSegment("openai", "v1", "chat", "completions")
                .build()
                .toUriString();
    }

    private Map<String, Object> requestBody(GroqCompletionRequest request) {
        return Map.of(
                "model", properties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", request.getSystemMessage()),
                        Map.of("role", "user", "content", request.getUserMessage())
                ),
                "temperature", properties.getTemperature(),
                "response_format", Map.of("type", "json_object")
        );
    }

    private String parseResponse(String responseBody) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (contentNode.isMissingNode() || contentNode.isNull() || !StringUtils.hasText(contentNode.asText())) {
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, "Groq 응답 본문이 비어 있습니다.");
        }
        return contentNode.asText();
    }
}
