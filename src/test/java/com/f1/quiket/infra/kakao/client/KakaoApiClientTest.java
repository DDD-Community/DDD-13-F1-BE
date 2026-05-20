package com.f1.quiket.infra.kakao.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import com.f1.quiket.infra.kakao.config.KakaoOAuthProperties;
import com.f1.quiket.infra.kakao.dto.KakaoUserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KakaoApiClientTest {

    private static final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

    private MockRestServiceServer mockServer;
    private KakaoApiClient kakaoApiClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();

        KakaoOAuthProperties properties = new KakaoOAuthProperties();
        properties.setUserInfoUri(USER_INFO_URI);
        kakaoApiClient = new KakaoApiClient(builder.build(), properties);
    }

    @Test
    void getUserInfo_extracts_provider_subject_email_and_nickname() {
        mockServer.expect(requestTo(USER_INFO_URI))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer kakao-access-token"))
                .andRespond(withSuccess("""
                        {
                          "id": 123456789,
                          "kakao_account": {
                            "is_email_valid": true,
                            "is_email_verified": true,
                            "email": "user@example.com",
                            "profile": {
                              "nickname": "도토리"
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        KakaoUserInfo response = kakaoApiClient.getUserInfo("kakao-access-token");

        assertThat(response.providerSubject()).isEqualTo("123456789");
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.emailValid()).isTrue();
        assertThat(response.emailVerified()).isTrue();
        assertThat(response.nickname()).isEqualTo("도토리");
        assertThat(response.hasUsableEmail()).isTrue();
    }

    @Test
    void getUserInfo_fails_when_kakao_token_invalid() {
        mockServer.expect(requestTo(USER_INFO_URI))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> kakaoApiClient.getUserInfo("invalid-token"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_OAUTH_INVALID_TOKEN);
    }
}
