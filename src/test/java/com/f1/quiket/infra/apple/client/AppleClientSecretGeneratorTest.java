package com.f1.quiket.infra.apple.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import com.f1.quiket.infra.apple.config.AppleOAuthProperties;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AppleClientSecretGeneratorTest {

    private static final String TEAM_ID = "TEAM123456";
    private static final String KEY_ID = "KEY1234567";
    private static final String CLIENT_ID = "com.f1.quiket.ios";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private KeyPair keyPair;
    private AppleClientSecretGenerator generator;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
        keyPair = keyPairGenerator.generateKeyPair();

        AppleOAuthProperties properties = new AppleOAuthProperties();
        properties.setTeamId(TEAM_ID);
        properties.setKeyId(KEY_ID);
        properties.setClientId(CLIENT_ID);
        properties.setPrivateKey(toPem(keyPair.getPrivate().getEncoded()));
        generator = new AppleClientSecretGenerator(properties);
    }

    @Test
    void generate_creates_es256_jwt_with_expected_claims() throws Exception {
        String clientSecret = generator.generate();
        String[] parts = clientSecret.split("\\.");

        assertThat(parts).hasSize(3);
        JsonNode header = objectMapper.readTree(decode(parts[0]));
        assertThat(header.path("alg").asText()).isEqualTo("ES256");
        assertThat(header.path("kid").asText()).isEqualTo(KEY_ID);

        JsonNode payload = objectMapper.readTree(decode(parts[1]));
        assertThat(payload.path("iss").asText()).isEqualTo(TEAM_ID);
        assertThat(payload.path("sub").asText()).isEqualTo(CLIENT_ID);
        assertThat(payload.path("aud").asText()).isEqualTo("https://appleid.apple.com");
        assertThat(payload.path("exp").asLong()).isGreaterThan(payload.path("iat").asLong());
    }

    @Test
    void generate_signature_verifies_with_public_key() throws Exception {
        String clientSecret = generator.generate();
        String[] parts = clientSecret.split("\\.");

        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initVerify(keyPair.getPublic());
        signature.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8));
        byte[] joseSignature = Base64.getUrlDecoder().decode(parts[2]);

        assertThat(joseSignature).hasSize(64);
        assertThat(signature.verify(joseToDer(joseSignature))).isTrue();
    }

    @Test
    void generate_throws_when_credentials_not_configured() {
        AppleClientSecretGenerator unconfigured = new AppleClientSecretGenerator(new AppleOAuthProperties());

        assertThatThrownBy(unconfigured::generate)
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_APPLE_CONFIG_ERROR));
    }

    private String toPem(byte[] pkcs8) {
        return "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8)).encodeToString(pkcs8)
                + "\n-----END PRIVATE KEY-----";
    }

    private byte[] decode(String base64Url) {
        return Base64.getUrlDecoder().decode(base64Url);
    }

    private byte[] joseToDer(byte[] jose) throws Exception {
        byte[] r = derInteger(Arrays.copyOfRange(jose, 0, 32));
        byte[] s = derInteger(Arrays.copyOfRange(jose, 32, 64));

        ByteArrayOutputStream der = new ByteArrayOutputStream();
        der.write(0x30);
        der.write(r.length + s.length);
        der.write(r);
        der.write(s);
        return der.toByteArray();
    }

    private byte[] derInteger(byte[] component) throws Exception {
        int offset = 0;
        while (offset < component.length - 1 && component[offset] == 0) {
            offset++;
        }
        byte[] trimmed = Arrays.copyOfRange(component, offset, component.length);

        ByteArrayOutputStream der = new ByteArrayOutputStream();
        der.write(0x02);
        boolean needsPadding = (trimmed[0] & 0x80) != 0;
        der.write(trimmed.length + (needsPadding ? 1 : 0));
        if (needsPadding) {
            der.write(0x00);
        }
        der.write(trimmed);
        return der.toByteArray();
    }
}
