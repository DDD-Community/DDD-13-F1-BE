package com.f1.quiket.infra.aoai.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.f1.quiket.domain.quiz.dto.QuizAiGenerationPrompt;
import com.f1.quiket.domain.quiz.dto.QuizAiGenerationResponse;
import com.f1.quiket.domain.quiz.service.QuizAiClient;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import com.f1.quiket.infra.aoai.config.AzureOpenAiProperties;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class AzureOpenAiQuizClient implements QuizAiClient {

    private static final String API_KEY_HEADER = "api-key";
    private static final String RESPONSE_FORMAT_NAME = "quiz_generation_response";

    private final RestClient azureOpenAiRestClient;
    private final AzureOpenAiProperties properties;
    private final ObjectMapper objectMapper;

    public AzureOpenAiQuizClient(
            @Qualifier("azureOpenAiRestClient") RestClient azureOpenAiRestClient,
            AzureOpenAiProperties properties
    ) {
        this.azureOpenAiRestClient = azureOpenAiRestClient;
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public QuizAiGenerationResponse generate(QuizAiGenerationPrompt prompt) {
        validateConfigured();
        try {
            String responseBody = azureOpenAiRestClient.post()
                    .uri(chatCompletionsUri())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(API_KEY_HEADER, properties.getApiKey())
                    .body(requestBody(prompt))
                    .retrieve()
                    .body(String.class);
            return parseResponse(responseBody);
        } catch (CustomException e) {
            throw e;
        } catch (RestClientException | JsonProcessingException e) {
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, "AOAI 퀴즈 생성 호출에 실패했습니다.", e);
        }
    }

    private void validateConfigured() {
        if (!properties.isConfigured()) {
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, "AOAI 설정값이 준비되지 않았습니다.");
        }
    }

    private String chatCompletionsUri() {
        String endpoint = properties.getEndpoint().replaceAll("/+$", "");
        return UriComponentsBuilder.fromUriString(endpoint)
                .pathSegment("openai", "deployments", properties.getDeploymentName(), "chat", "completions")
                .queryParam("api-version", properties.getApiVersion())
                .build()
                .toUriString();
    }

    private Map<String, Object> requestBody(QuizAiGenerationPrompt prompt) {
        return Map.of(
                "messages", List.of(
                        Map.of("role", "system", "content", prompt.systemMessage()),
                        Map.of("role", "user", "content", prompt.userMessage())
                ),
                "temperature", properties.getTemperature(),
                "max_tokens", properties.getMaxOutputTokens(),
                "response_format", responseFormat()
        );
    }

    private Map<String, Object> responseFormat() {
        return Map.of(
                "type", "json_schema",
                "json_schema", Map.of(
                        "name", RESPONSE_FORMAT_NAME,
                        "strict", true,
                        "schema", responseSchema()
                )
        );
    }

    private Map<String, Object> responseSchema() {
        Map<String, Object> optionSchema = new LinkedHashMap<>();
        optionSchema.put("type", "object");
        optionSchema.put("additionalProperties", false);
        optionSchema.put("required", List.of("optionNumber", "content"));
        optionSchema.put("properties", Map.of(
                "optionNumber", Map.of("type", "integer", "minimum", 1, "maximum", 5),
                "content", Map.of("type", "string", "minLength", 1)
        ));

        Map<String, Object> questionSchema = new LinkedHashMap<>();
        questionSchema.put("type", "object");
        questionSchema.put("additionalProperties", false);
        questionSchema.put("required", List.of(
                "partId",
                "questionType",
                "difficulty",
                "summary",
                "body",
                "options",
                "answerValue",
                "correctExplanation",
                "incorrectExplanation"
        ));
        questionSchema.put("properties", Map.of(
                "partId", Map.of("type", "string", "minLength", 1),
                "questionType", Map.of("type", "string", "enum", List.of("multiple_choice", "ox")),
                "difficulty", Map.of("type", "string", "enum", List.of("easy", "medium", "hard")),
                "summary", Map.of("type", "string", "minLength", 8, "maxLength", 20),
                "body", Map.of("type", "string", "minLength", 1),
                "options", Map.of("type", "array", "items", optionSchema),
                "answerValue", Map.of("type", "string", "minLength", 1, "maxLength", 10),
                "correctExplanation", Map.of("type", "string", "minLength", 5),
                "incorrectExplanation", Map.of("type", "string", "minLength", 5)
        ));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", List.of("questions"));
        schema.put("properties", Map.of(
                "questions", Map.of(
                        "type", "array",
                        "minItems", 1,
                        "maxItems", 100,
                        "items", questionSchema
                )
        ));
        return schema;
    }

    private QuizAiGenerationResponse parseResponse(String responseBody) throws JsonProcessingException {
        JsonNode rootNode = objectMapper.readTree(responseBody);
        JsonNode contentNode = rootNode.path("choices").path(0).path("message").path("content");
        if (contentNode.isMissingNode() || contentNode.isNull()) {
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, "AOAI 퀴즈 생성 응답이 비어 있습니다.");
        }
        return objectMapper.readValue(contentNode.asText(), QuizAiGenerationResponse.class);
    }
}
