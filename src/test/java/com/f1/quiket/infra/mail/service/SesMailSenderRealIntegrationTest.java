package com.f1.quiket.infra.mail.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.f1.quiket.infra.mail.config.AwsSesProperties;
import com.f1.quiket.infra.mail.dto.MailSendRequest;
import com.f1.quiket.infra.mail.template.MailTemplateFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.StringUtils;

/**
 * SES 실발송 수동 통합 테스트
 *
 * 실행 방법
 * 0. AWS 자격증명 설정
 * export AWS_ACCESS_KEY_ID=<AWS access key id>
 * export AWS_SECRET_ACCESS_KEY=<AWS secret access key>
 * export AWS_REGION=ap-northeast-2
 * 또는 ~/.aws/credentials, ~/.aws/config에 프로파일 구성
 *
 * 1. 환경변수 설정
 * export SES_REAL_MAIL_TEST_ENABLED=true
 * export AWS_SES_TEST_FROM_EMAIL=<SES 검증된 발신 이메일>
 * export AWS_SES_TEST_TO_EMAIL=<수신 이메일>
 *
 * 2. 단일 테스트 실행
 * ./gradlew test --tests "com.f1.quiket.infra.mail.service.SesMailSenderRealIntegrationTest"
 *
 * 참고
 * - SES_REAL_MAIL_TEST_ENABLED=true가 아니면 테스트는 skip 처리
 * - 발신자/수신자 환경변수가 없으면 테스트는 skip 처리
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("manual")
@Slf4j
class SesMailSenderRealIntegrationTest {

    private static final String ENABLED_ENV = "SES_REAL_MAIL_TEST_ENABLED";
    private static final String FROM_EMAIL_ENV = "AWS_SES_TEST_FROM_EMAIL";
    private static final String TO_EMAIL_ENV = "AWS_SES_TEST_TO_EMAIL";

    @Autowired
    private AwsSesProperties awsSesProperties;

    @Autowired
    private SesMailSender sesMailSender;

    @Autowired
    private MailTemplateFactory mailTemplateFactory;

    @DynamicPropertySource
    static void overrideFromEmail(DynamicPropertyRegistry registry) {
        String fromEmail = System.getenv(FROM_EMAIL_ENV);
        if (StringUtils.hasText(fromEmail)) {
            registry.add("aws.ses.from-email", () -> fromEmail);
        }
    }

    @Test
    void sendMail_realSes() {
        progress("start");
        String enabled = System.getenv(ENABLED_ENV);
        if (!"true".equalsIgnoreCase(enabled)) {
            progress("skipped: set " + ENABLED_ENV + "=true to run real SES test");
        }
        assumeTrue(
                "true".equalsIgnoreCase(enabled),
                () -> "[SES REAL TEST] skipped: set " + ENABLED_ENV + "=true to run real SES test"
        );
        progress("enabled env verified: " + ENABLED_ENV + "=" + enabled);

        String fromEmail = System.getenv(FROM_EMAIL_ENV);
        String toEmail = System.getenv(TO_EMAIL_ENV);
        if (!StringUtils.hasText(fromEmail)) {
            progress("skipped: missing env " + FROM_EMAIL_ENV);
        }
        assumeTrue(StringUtils.hasText(fromEmail), () -> "[SES REAL TEST] skipped: missing env " + FROM_EMAIL_ENV);
        if (!StringUtils.hasText(toEmail)) {
            progress("skipped: missing env " + TO_EMAIL_ENV);
        }
        assumeTrue(StringUtils.hasText(toEmail), () -> "[SES REAL TEST] skipped: missing env " + TO_EMAIL_ENV);

        progress("env loaded. fromEmail=" + maskEmail(fromEmail) + ", toEmail=" + maskEmail(toEmail));
        assertTrue(fromEmail.equals(awsSesProperties.getFromEmail()), "from-email should be loaded from env override");
        progress(
                "aws.ses resolved. region=" + awsSesProperties.getRegion()
                        + ", fromEmail=" + maskEmail(awsSesProperties.getFromEmail())
        );

        progress("attempting send template mail");
        MailSendRequest mailRequest = mailTemplateFactory.createSignUpVerificationMail(toEmail, "123456");
        String messageId = sesMailSender.sendMail(mailRequest);

        assertTrue(StringUtils.hasText(messageId), "messageId should not be blank");
        progress("success. messageId=" + messageId);
    }

    private static String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return "(blank)";
        }
        String[] parts = email.split("@", 2);
        String localPart = parts[0];
        String domainPart = parts[1];
        String maskedLocalPart = localPart.length() <= 2
                ? "*".repeat(localPart.length())
                : localPart.substring(0, 2) + "*".repeat(localPart.length() - 2);
        return maskedLocalPart + "@" + domainPart;
    }

    private void progress(String message) {
        String fullMessage = "[SES REAL TEST] " + message;
        log.info(fullMessage);
        System.out.println(fullMessage);
    }
}
