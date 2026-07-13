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
    private static final String TITLE_PLACEHOLDER = "{{title}}";
    private static final String DESCRIPTION_PLACEHOLDER = "{{description}}";
    private static final String VERIFICATION_CODE_PLACEHOLDER = "{{verificationCode}}";

    private static final String COMMON_TITLE = "이메일 인증코드";
    private static final String SIGN_UP_DESCRIPTION =
            "AI 퀴즈로 채워지는 나만의 도토리 창고,<br />"
                    + "'퀴켓(Quiket)'에 가입하신 것을 환영해요!<br /><br />"
                    + "아래의 인증코드를 퀴켓 앱에 입력해주시면 회원가입이 완료돼요.";
    private static final String PASSWORD_RESET_DESCRIPTION =
            "비밀번호 재설정을 요청하셨어요.<br />"
                    + "아래의 인증코드를 퀴켓 앱에 입력해주시면 비밀번호 재설정이 완료돼요.";
    private static final String EMAIL_CHANGE_DESCRIPTION =
            "이메일 변경을 요청하셨어요.<br />"
                    + "아래의 인증코드를 퀴켓 앱에 입력해주시면 이메일 변경이 완료돼요.";

    private final ResourceLoader resourceLoader;

    public MailSendRequest createSignUpVerificationMail(String toEmail, String verificationCode) {
        String subject = "[Quiket] 회원가입 이메일 인증";
        String body = createVerificationCodeBody(COMMON_TITLE, SIGN_UP_DESCRIPTION, verificationCode);
        return MailSendRequest.html(toEmail, subject, body);
    }

    public MailSendRequest createPasswordResetMail(String toEmail, String verificationCode) {
        String subject = "[Quiket] 비밀번호 재설정 이메일 인증";
        String body = createVerificationCodeBody(COMMON_TITLE, PASSWORD_RESET_DESCRIPTION, verificationCode);
        return MailSendRequest.html(toEmail, subject, body);
    }

    public MailSendRequest createEmailChangeMail(String toEmail, String verificationCode) {
        String subject = "[Quiket] 이메일 변경 인증";
        String body = createVerificationCodeBody(COMMON_TITLE, EMAIL_CHANGE_DESCRIPTION, verificationCode);
        return MailSendRequest.html(toEmail, subject, body);
    }

    private String createVerificationCodeBody(String title, String description, String verificationCode) {
        return loadTemplate(VERIFICATION_TEMPLATE_PATH)
                .replace(TITLE_PLACEHOLDER, title)
                .replace(DESCRIPTION_PLACEHOLDER, description)
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
