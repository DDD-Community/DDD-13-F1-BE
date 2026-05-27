package com.f1.quiket.infra.groq.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import com.f1.quiket.infra.groq.config.GroqProperties;
import com.f1.quiket.infra.groq.dto.GroqCompletionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GroqClientTest {

    private static final String URI = "https://api.groq.com/openai/v1/chat/completions";

    private MockRestServiceServer mockServer;
    private GroqClient groqClient;
    private GroqProperties properties;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        properties = properties();
        groqClient = new GroqClient(builder.build(), properties);
    }

    @Test
    void generate_calls_groq_and_parses_response() {
        mockServer.expect(requestTo(URI))
                .andExpect(header("Authorization", "Bearer groq-key"))
                .andExpect(content().string(containsString("\"response_format\"")))
                .andRespond(withSuccess("""
                        {
                          "choices": [
                            {
                              "message": {
                                "content": "{\\"parts\\":[{\\"partNumber\\":1,\\"name\\":\\"정규화\\",\\"content\\":\\"본문\\"}]}"
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        String response = groqClient.generate(
                        GroqCompletionRequest.builder()
                                .systemMessage("system")
                                .userMessage("user")
                                .build()
                )
                .getContent();

        assertThat(response).contains("\"parts\"");
        mockServer.verify();
    }

    @Test
    void generate_throws_when_properties_missing() {
        properties.setApiKey("");

        assertThatThrownBy(() -> groqClient.generate(
                GroqCompletionRequest.builder().systemMessage("system").userMessage("user").build()
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
    }

    private GroqProperties properties() {
        GroqProperties properties = new GroqProperties();
        properties.setBaseUrl("https://api.groq.com");
        properties.setApiKey("groq-key");
        properties.setModel("llama-3.3-70b-versatile");
        properties.setTemperature(0.2);
        return properties;
    }
}
