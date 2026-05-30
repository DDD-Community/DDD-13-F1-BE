package com.f1.quiket.infra.mail.listener;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.auth.event.EmailVerificationMailRequestedEvent;
import com.f1.quiket.domain.auth.event.PasswordResetMailRequestedEvent;
import com.f1.quiket.domain.mypage.event.MyEmailChangeMailRequestedEvent;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import com.f1.quiket.infra.mail.dto.MailSendRequest;
import com.f1.quiket.infra.mail.service.SesMailSender;
import com.f1.quiket.infra.mail.template.MailTemplateFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmailVerificationMailEventListenerTest {

    private MailTemplateFactory mailTemplateFactory;
    private SesMailSender sesMailSender;
    private EmailVerificationMailEventListener listener;

    @BeforeEach
    void setUp() {
        mailTemplateFactory = mock(MailTemplateFactory.class);
        sesMailSender = mock(SesMailSender.class);
        listener = new EmailVerificationMailEventListener(mailTemplateFactory, sesMailSender);
    }

    @Test
    void sendPasswordResetMail_does_not_propagate_when_send_fails() {
        // 메일 발송 실패가 AFTER_COMMIT을 통해 호출자(컨트롤러)로 전파되면 정상 요청이 5xx가 된다.
        // 리스너는 발송 예외를 격리하고 전파하지 않아야 한다.
        when(mailTemplateFactory.createPasswordResetMail(any(), any())).thenReturn(mock(MailSendRequest.class));
        doThrow(new CustomException(ErrorCode.MAIL_SEND_FAILED)).when(sesMailSender).sendMail(any());

        assertThatCode(() -> listener.sendPasswordResetMail(
                new PasswordResetMailRequestedEvent("user@example.com", "123456")
        )).doesNotThrowAnyException();

        verify(sesMailSender).sendMail(any());
    }

    @Test
    void sendEmailVerificationMail_does_not_propagate_when_send_fails() {
        when(mailTemplateFactory.createSignUpVerificationMail(any(), any())).thenReturn(mock(MailSendRequest.class));
        doThrow(new CustomException(ErrorCode.MAIL_SEND_FAILED)).when(sesMailSender).sendMail(any());

        assertThatCode(() -> listener.sendEmailVerificationMail(
                new EmailVerificationMailRequestedEvent("user@example.com", "123456")
        )).doesNotThrowAnyException();

        verify(sesMailSender).sendMail(any());
    }

    @Test
    void sendMyEmailChangeMail_does_not_propagate_when_send_fails() {
        when(mailTemplateFactory.createEmailChangeMail(any(), any())).thenReturn(mock(MailSendRequest.class));
        doThrow(new RuntimeException("ses down")).when(sesMailSender).sendMail(any());

        assertThatCode(() -> listener.sendMyEmailChangeMail(
                new MyEmailChangeMailRequestedEvent("user@example.com", "123456")
        )).doesNotThrowAnyException();

        verify(sesMailSender).sendMail(any());
    }

    @Test
    void sendPasswordResetMail_sends_mail_on_success() {
        MailSendRequest request = mock(MailSendRequest.class);
        when(mailTemplateFactory.createPasswordResetMail(any(), any())).thenReturn(request);

        listener.sendPasswordResetMail(new PasswordResetMailRequestedEvent("user@example.com", "123456"));

        verify(sesMailSender).sendMail(request);
    }
}
