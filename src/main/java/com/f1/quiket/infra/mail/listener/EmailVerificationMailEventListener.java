package com.f1.quiket.infra.mail.listener;

import com.f1.quiket.domain.auth.event.EmailVerificationMailRequestedEvent;
import com.f1.quiket.domain.auth.event.PasswordResetMailRequestedEvent;
import com.f1.quiket.domain.mypage.event.MyEmailChangeMailRequestedEvent;
import com.f1.quiket.infra.mail.dto.MailSendRequest;
import com.f1.quiket.infra.mail.service.SesMailSender;
import com.f1.quiket.infra.mail.template.MailTemplateFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EmailVerificationMailEventListener {

    private final MailTemplateFactory mailTemplateFactory;
    private final SesMailSender sesMailSender;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void sendEmailVerificationMail(EmailVerificationMailRequestedEvent event) {
        MailSendRequest mailRequest = mailTemplateFactory.createSignUpVerificationMail(
                event.email(),
                event.verificationCode()
        );
        sesMailSender.sendMail(mailRequest);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void sendPasswordResetMail(PasswordResetMailRequestedEvent event) {
        MailSendRequest mailRequest = mailTemplateFactory.createPasswordResetMail(
                event.email(),
                event.verificationCode()
        );
        sesMailSender.sendMail(mailRequest);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void sendMyEmailChangeMail(MyEmailChangeMailRequestedEvent event) {
        MailSendRequest mailRequest = mailTemplateFactory.createEmailChangeMail(
                event.email(),
                event.verificationCode()
        );
        sesMailSender.sendMail(mailRequest);
    }
}
