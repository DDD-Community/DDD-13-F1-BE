package com.f1.quiket.infra.aoai.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.f1.quiket.domain.quiz.dto.QuizAiGenerationPrompt;
import com.f1.quiket.domain.quiz.dto.QuizAiGenerationResponse;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import com.f1.quiket.infra.aoai.config.AzureOpenAiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AzureOpenAiQuizClientTest {

    private static final String CHAT_COMPLETIONS_URI =
            "https://quiket-aoai.openai.azure.com/openai/deployments/quiz-deployment/chat/completions?api-version=2024-10-21";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockRestServiceServer mockServer;
    private AzureOpenAiQuizClient azureOpenAiQuizClient;
    private AzureOpenAiProperties properties;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        properties = properties();
        azureOpenAiQuizClient = new AzureOpenAiQuizClient(builder.build(), properties);
    }

    @Test
    void generate_calls_azure_openai_and_parses_structured_output() throws Exception {
        mockServer.expect(requestTo(CHAT_COMPLETIONS_URI))
                .andExpect(header("api-key", "aoai-key"))
                .andExpect(content().string(containsString("json_schema")))
                .andExpect(content().string(containsString("quiz_generation_response")))
                .andRespond(withSuccess(chatCompletionResponse(), MediaType.APPLICATION_JSON));

        QuizAiGenerationResponse response = azureOpenAiQuizClient.generate(
                new QuizAiGenerationPrompt("system", "user")
        );

        assertThat(response.getQuestions()).hasSize(1);
        assertThat(response.getQuestions().get(0).getPartId()).isEqualTo("part-public-id");
        assertThat(response.getQuestions().get(0).getAnswerValue()).isEqualTo("1");
        mockServer.verify();
    }

    @Test
    void generate_throws_service_unavailable_when_properties_missing() {
        properties.setApiKey("");

        assertThatThrownBy(() -> azureOpenAiQuizClient.generate(new QuizAiGenerationPrompt("system", "user")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
    }

    private AzureOpenAiProperties properties() {
        AzureOpenAiProperties properties = new AzureOpenAiProperties();
        properties.setEndpoint("https://quiket-aoai.openai.azure.com");
        properties.setApiKey("aoai-key");
        properties.setDeploymentName("quiz-deployment");
        properties.setApiVersion("2024-10-21");
        properties.setMaxOutputTokens(4096);
        properties.setTemperature(0.2);
        return properties;
    }

    private String chatCompletionResponse() throws JsonProcessingException {
        String content = """
                {
                  "questions": [
                    {
                      "partId": "part-public-id",
                      "questionType": "multiple_choice",
                      "difficulty": "medium",
                      "summary": "정규화 핵심 개념",
                      "body": "정규화의 목적은 무엇인가요?",
                      "options": [
                        {"optionNumber": 1, "content": "중복 최소화"},
                        {"optionNumber": 2, "content": "중복 최대화"},
                        {"optionNumber": 3, "content": "테이블 삭제"},
                        {"optionNumber": 4, "content": "인덱스 제거"}
                      ],
                      "answerValue": "1",
                      "correctExplanation": "정규화는 데이터 중복을 줄입니다.",
                      "incorrectExplanation": "다른 선택지는 정규화 목적과 다릅니다."
                    }
                  ]
                }
                """;
        return """
                {
                  "choices": [
                    {
                      "message": {
                        "content": %s
                      }
                    }
                  ]
                }
                """.formatted(objectMapper.writeValueAsString(content));
    }
}
