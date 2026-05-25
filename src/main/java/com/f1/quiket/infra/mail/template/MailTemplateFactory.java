package com.f1.quiket.infra.mail.template;

import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import com.f1.quiket.infra.mail.dto.MailSendRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

@Component
@RequiredArgsConstructor
public class MailTemplateFactory {

    private static final String VERIFICATION_TEMPLATE_PATH = "classpath:mail-template/verification-code.html";
    private static final String VERIFICATION_CODE_PLACEHOLDER = "{{verificationCode}}";

    private final ResourceLoader resourceLoader;

    public MailSendRequest createSignUpVerificationMail(String toEmail, String verificationCode) {
        String subject = "[Quiket] 회원가입 이메일 인증";
        String body = createVerificationCodeBody(verificationCode);
        return MailSendRequest.html(toEmail, subject, body);
    }

    public MailSendRequest createPasswordResetMail(String toEmail, String verificationCode) {
        String subject = "[Quiket] 비밀번호 재설정 이메일 인증";
        String body = createVerificationCodeBody(verificationCode);
        return MailSendRequest.html(toEmail, subject, body);
    }

    public MailSendRequest createEmailChangeMail(String toEmail, String verificationCode) {
        String subject = "[Quiket] 이메일 변경 인증";
        String body = createVerificationCodeBody(verificationCode);
        return MailSendRequest.html(toEmail, subject, body);
    }

    private String createVerificationCodeBody(String verificationCode) {
        return loadTemplate(VERIFICATION_TEMPLATE_PATH)
                .replace(VERIFICATION_CODE_PLACEHOLDER, verificationCode);
    }

    private String loadTemplate(String templatePath) {
        Resource resource = resourceLoader.getResource(templatePath);
        try (InputStream inputStream = resource.getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "메일 템플릿 로딩 실패", e);
        }
    }
}
