package com.f1.quiket.infra.mail.listener;

import com.f1.quiket.domain.auth.event.EmailVerificationMailRequestedEvent;
import com.f1.quiket.domain.auth.event.PasswordResetMailRequestedEvent;
import com.f1.quiket.domain.mypage.event.MyEmailChangeMailRequestedEvent;
import com.f1.quiket.infra.mail.dto.MailSendRequest;
import com.f1.quiket.infra.mail.service.SesMailSender;
import com.f1.quiket.infra.mail.template.MailTemplateFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 인증/비밀번호 재설정/이메일 변경 메일 발송 리스너.
 *
 * <p>모든 메일 발송은 {@code @TransactionalEventListener(AFTER_COMMIT)}로 처리한다.
 * AFTER_COMMIT 단계에서 던진 예외는 트랜잭션 동기화 콜백을 통해 호출자(컨트롤러)까지
 * 전파되어 이미 정상 처리된 요청을 5xx로 만들 수 있다. 메일 발송은 요청 처리의 부가
 * 작업이므로 실패해도 본 요청(인증 코드 발급 등)은 성공으로 응답해야 한다.
 * 따라서 발송 예외를 여기서 격리하고 로그로만 남긴다.</p>
 */
@Slf4j
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
        sendSafely(mailRequest, "sign-up-verification", event.email());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void sendPasswordResetMail(PasswordResetMailRequestedEvent event) {
        MailSendRequest mailRequest = mailTemplateFactory.createPasswordResetMail(
                event.email(),
                event.verificationCode()
        );
        sendSafely(mailRequest, "password-reset", event.email());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void sendMyEmailChangeMail(MyEmailChangeMailRequestedEvent event) {
        MailSendRequest mailRequest = mailTemplateFactory.createEmailChangeMail(
                event.email(),
                event.verificationCode()
        );
        sendSafely(mailRequest, "my-email-change", event.email());
    }

    /**
     * 메일 발송 실패를 격리한다. 발송 실패는 본 요청 처리에 영향을 주지 않도록
     * 예외를 전파하지 않고 로그로만 기록한다 (AFTER_COMMIT 예외의 호출자 전파 차단).
     */
    private void sendSafely(MailSendRequest mailRequest, String mailType, String toEmail) {
        try {
            sesMailSender.sendMail(mailRequest);
        } catch (Exception e) {
            log.error("메일 발송 실패 — 요청 처리는 계속됩니다. mailType={}, toEmail={}", mailType, toEmail, e);
        }
    }
}
