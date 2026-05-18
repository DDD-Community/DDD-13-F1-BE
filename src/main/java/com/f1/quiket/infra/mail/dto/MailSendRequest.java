package com.f1.quiket.infra.mail.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

/**
 * 메일 발송 요청
 */
@Getter
@Builder
public class MailSendRequest {

    @Singular("toEmail")
    private List<String> toEmails;
    private String subject;
    private String body;
    private MailContentType contentType;

    public static MailSendRequest text(String toEmail, String subject, String body) {
        return MailSendRequest.builder()
                .toEmail(toEmail)
                .subject(subject)
                .body(body)
                .contentType(MailContentType.TEXT)
                .build();
    }

    public static MailSendRequest html(String toEmail, String subject, String body) {
        return MailSendRequest.builder()
                .toEmail(toEmail)
                .subject(subject)
                .body(body)
                .contentType(MailContentType.HTML)
                .build();
    }
}
