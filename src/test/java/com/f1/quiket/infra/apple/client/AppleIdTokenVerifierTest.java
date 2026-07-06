package com.f1.quiket.infra.apple.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import com.f1.quiket.infra.apple.config.AppleOAuthProperties;
import com.f1.quiket.infra.apple.dto.AppleUserInfo;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AppleIdTokenVerifierTest {

    private static final String JWKS_URI = "https://appleid.apple.com/auth/keys";
    private static final String CLIENT_ID = "com.f1.quiket.ios";
    private static final String KEY_ID = "test-key-id";

    private MockRestServiceServer mockServer;
    private AppleIdTokenVerifier verifier;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();

        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();

        AppleOAuthProperties properties = new AppleOAuthProperties();
        properties.setClientId(CLIENT_ID);
        verifier = new AppleIdTokenVerifier(builder.build(), properties);
    }

    @Test
    void verify_extracts_subject_email_and_verified_flag() throws Exception {
        expectJwks(ExpectedCount.once());
        String token = identityToken(CLIENT_ID, Instant.now().plusSeconds(600), KEY_ID);

        AppleUserInfo userInfo = verifier.verify(token);

        assertThat(userInfo.providerSubject()).isEqualTo("001234.abcdef");
        assertThat(userInfo.email()).isEqualTo("user@privaterelay.appleid.com");
        assertThat(userInfo.hasUsableEmail()).isTrue();
        mockServer.verify();
    }

    @Test
    void verify_caches_jwks_between_calls() throws Exception {
        expectJwks(ExpectedCount.once());
        String token = identityToken(CLIENT_ID, Instant.now().plusSeconds(600), KEY_ID);

        verifier.verify(token);
        verifier.verify(token);

        mockServer.verify();
    }

    @Test
    void verify_throws_when_audience_mismatched() throws Exception {
        expectJwks(ExpectedCount.once());
        String token = identityToken("com.other.app", Instant.now().plusSeconds(600), KEY_ID);

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_APPLE_INVALID_TOKEN));
    }

    @Test
    void verify_throws_when_token_expired() throws Exception {
        expectJwks(ExpectedCount.once());
        String token = identityToken(CLIENT_ID, Instant.now().minusSeconds(60), KEY_ID);

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_APPLE_INVALID_TOKEN));
    }

    @Test
    void verify_throws_when_signature_tampered() throws Exception {
        expectJwks(ExpectedCount.once());
        String token = identityToken(CLIENT_ID, Instant.now().plusSeconds(600), KEY_ID);
        String tampered = token.substring(0, token.length() - 4) + "AAAA";

        assertThatThrownBy(() -> verifier.verify(tampered))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_APPLE_INVALID_TOKEN));
    }

    @Test
    void verify_throws_when_kid_unknown() throws Exception {
        expectJwks(ExpectedCount.once());
        String token = identityToken(CLIENT_ID, Instant.now().plusSeconds(600), "unknown-kid");

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_APPLE_INVALID_TOKEN));
    }

    @Test
    void verify_throws_when_client_id_not_configured() {
        AppleOAuthProperties unconfigured = new AppleOAuthProperties();
        AppleIdTokenVerifier unconfiguredVerifier =
                new AppleIdTokenVerifier(RestClient.builder().build(), unconfigured);

        assertThatThrownBy(() -> unconfiguredVerifier.verify("any-token"))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_APPLE_CONFIG_ERROR));
    }

    private void expectJwks(ExpectedCount count) {
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        String jwks = """
                {
                  "keys": [
                    {
                      "kty": "RSA",
                      "kid": "%s",
                      "use": "sig",
                      "alg": "RS256",
                      "n": "%s",
                      "e": "%s"
                    }
                  ]
                }
                """.formatted(
                KEY_ID,
                base64Url(publicKey.getModulus()),
                base64Url(publicKey.getPublicExponent())
        );
        mockServer.expect(count, requestTo(JWKS_URI))
                .andRespond(withSuccess(jwks, MediaType.APPLICATION_JSON));
    }

    private String identityToken(String audience, Instant expiresAt, String kid) throws Exception {
        String header = encode("""
                {"alg":"RS256","kid":"%s"}""".formatted(kid));
        String payload = encode("""
                {
                  "iss": "https://appleid.apple.com",
                  "aud": "%s",
                  "exp": %d,
                  "sub": "001234.abcdef",
                  "email": "user@privaterelay.appleid.com",
                  "email_verified": "true"
                }""".formatted(audience, expiresAt.getEpochSecond()));

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update((header + "." + payload).getBytes(StandardCharsets.UTF_8));
        String encodedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());
        return header + "." + payload + "." + encodedSignature;
    }

    private String encode(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private String base64Url(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
