package com.f1.quiket.infra.mail.template;

import static org.assertj.core.api.Assertions.assertThat;

import com.f1.quiket.infra.mail.dto.MailSendRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

class MailTemplateFactoryTest {

    private MailTemplateFactory mailTemplateFactory;

    @BeforeEach
    void setUp() {
        mailTemplateFactory = new MailTemplateFactory(new DefaultResourceLoader());
    }

    @Test
    void createSignUpVerificationMail_replacesAllPlaceholders() {
        MailSendRequest request = mailTemplateFactory.createSignUpVerificationMail("user@example.com", "123456");

        assertThat(request.getBody()).contains("123456");
        assertThat(request.getBody()).doesNotContain("{{title}}", "{{description}}", "{{verificationCode}}");
    }

    @Test
    void createPasswordResetMail_replacesAllPlaceholders() {
        MailSendRequest request = mailTemplateFactory.createPasswordResetMail("user@example.com", "654321");

        assertThat(request.getBody()).contains("654321");
        assertThat(request.getBody()).doesNotContain("{{title}}", "{{description}}", "{{verificationCode}}");
    }

    @Test
    void createEmailChangeMail_replacesAllPlaceholders() {
        MailSendRequest request = mailTemplateFactory.createEmailChangeMail("user@example.com", "999888");

        assertThat(request.getBody()).contains("999888");
        assertThat(request.getBody()).doesNotContain("{{title}}", "{{description}}", "{{verificationCode}}");
    }

    @Test
    void allMailTypes_containDistinctDescriptions() {
        String signUpBody = mailTemplateFactory.createSignUpVerificationMail("a@a.com", "000000").getBody();
        String passwordResetBody = mailTemplateFactory.createPasswordResetMail("a@a.com", "000000").getBody();
        String emailChangeBody = mailTemplateFactory.createEmailChangeMail("a@a.com", "000000").getBody();

        assertThat(signUpBody).isNotEqualTo(passwordResetBody);
        assertThat(signUpBody).isNotEqualTo(emailChangeBody);
        assertThat(passwordResetBody).isNotEqualTo(emailChangeBody);
    }
}
