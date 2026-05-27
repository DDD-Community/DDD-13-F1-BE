package com.f1.quiket.infra.gemini.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import com.f1.quiket.infra.gemini.config.GeminiProperties;
import com.f1.quiket.infra.gemini.dto.GeminiBinaryData;
import com.f1.quiket.infra.gemini.dto.GeminiCompletionRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiClientTest {

    private static final String URI =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=gemini-key";

    private MockRestServiceServer mockServer;
    private GeminiClient geminiClient;
    private GeminiProperties properties;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        properties = properties();
        geminiClient = new GeminiClient(builder.build(), properties);
    }

    @Test
    void generate_calls_gemini_and_parses_response() {
        mockServer.expect(requestTo(URI))
                .andExpect(content().string(containsString("responseMimeType")))
                .andExpect(content().string(containsString("inlineData")))
                .andRespond(withSuccess("""
                        {
                          "candidates": [
                            {
                              "content": {
                                "parts": [
                                  {"text":"{\\"parts\\":[{\\"partNumber\\":1,\\"name\\":\\"데이터 모델\\",\\"content\\":\\"본문\\"}]}"}
                                ]
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        String response = geminiClient.generate(
                        GeminiCompletionRequest.builder()
                                .systemMessage("system")
                                .userMessage("user")
                                .build(),
                        List.of(GeminiBinaryData.builder()
                                .mimeType("image/png")
                                .bytes("img".getBytes(StandardCharsets.UTF_8))
                                .build())
                )
                .getContent();

        assertThat(response).contains("\"parts\"");
        mockServer.verify();
    }

    @Test
    void generate_throws_when_properties_missing() {
        properties.setApiKey("");

        assertThatThrownBy(() -> geminiClient.generate(
                GeminiCompletionRequest.builder().systemMessage("system").userMessage("user").build(),
                List.of()
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
    }

    private GeminiProperties properties() {
        GeminiProperties properties = new GeminiProperties();
        properties.setBaseUrl("https://generativelanguage.googleapis.com");
        properties.setApiKey("gemini-key");
        properties.setModel("gemini-3.1-flash-lite");
        properties.setTemperature(0.2);
        return properties;
    }
}
