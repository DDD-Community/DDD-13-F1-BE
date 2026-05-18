package com.f1.quiket.infra.mail.service;

import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import com.f1.quiket.infra.mail.config.AwsSesProperties;
import com.f1.quiket.infra.mail.dto.MailContentType;
import com.f1.quiket.infra.mail.dto.MailSendRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

/**
 * SES 메일 발송기
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SesMailSender {

    private static final String UTF_8 = "UTF-8";

    private final SesV2Client sesV2Client;
    private final AwsSesProperties awsSesProperties;

    public String sendTextMail(String toEmail, String subject, String body) {
        return sendMail(MailSendRequest.text(toEmail, subject, body));
    }

    public String sendHtmlMail(String toEmail, String subject, String body) {
        return sendMail(MailSendRequest.html(toEmail, subject, body));
    }

    public String sendMail(MailSendRequest request) {
        validateRequest(request);

        SendEmailRequest sendEmailRequest = createSendEmailRequest(request);
        log.info(
                "Sending SES mail. region={}, fromEmail={}, toEmails={}, subject={}, contentType={}",
                awsSesProperties.getRegion(),
                awsSesProperties.getFromEmail(),
                request.getToEmails(),
                request.getSubject(),
                request.getContentType()
        );

        try {
            SendEmailResponse response = sesV2Client.sendEmail(sendEmailRequest);
            log.info("SES mail sent successfully. messageId={}", response.messageId());
            return response.messageId();
        } catch (SesV2Exception e) {
            String errorCode = e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "unknown";
            String errorMessage = e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage();
            log.error(
                    "Failed to send SES mail by service exception. region={}, fromEmail={}, toEmails={}, subject={}, statusCode={}, requestId={}, errorCode={}, errorMessage={}",
                    awsSesProperties.getRegion(),
                    awsSesProperties.getFromEmail(),
                    request.getToEmails(),
                    request.getSubject(),
                    e.statusCode(),
                    e.requestId(),
                    errorCode,
                    errorMessage,
                    e
            );
            throw new CustomException(ErrorCode.MAIL_SEND_FAILED, e);
        } catch (SdkClientException e) {
            log.error(
                    "Failed to send SES mail by client exception. region={}, fromEmail={}, toEmails={}, subject={}, message={}",
                    awsSesProperties.getRegion(),
                    awsSesProperties.getFromEmail(),
                    request.getToEmails(),
                    request.getSubject(),
                    e.getMessage(),
                    e
            );
            throw new CustomException(ErrorCode.MAIL_SEND_FAILED, e);
        }
    }

    private SendEmailRequest createSendEmailRequest(MailSendRequest request) {
        Content subject = Content.builder()
                .data(request.getSubject())
                .charset(UTF_8)
                .build();

        Content bodyContent = Content.builder()
                .data(request.getBody())
                .charset(UTF_8)
                .build();

        Body body = request.getContentType() == MailContentType.HTML
                ? Body.builder().html(bodyContent).build()
                : Body.builder().text(bodyContent).build();

        Message message = Message.builder()
                .subject(subject)
                .body(body)
                .build();

        return SendEmailRequest.builder()
                .fromEmailAddress(awsSesProperties.getFromEmail())
                .destination(Destination.builder().toAddresses(request.getToEmails()).build())
                .content(EmailContent.builder().simple(message).build())
                .build();
    }

    private void validateRequest(MailSendRequest request) {
        if (request == null || CollectionUtils.isEmpty(request.getToEmails())
                || !StringUtils.hasText(request.getSubject())
                || !StringUtils.hasText(request.getBody())
                || request.getContentType() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Invalid mail request.");
        }

        List<String> toEmails = request.getToEmails();
        boolean hasInvalidEmail = toEmails.stream().anyMatch(email -> !StringUtils.hasText(email));
        if (hasInvalidEmail) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Invalid recipient email.");
        }
    }
}
