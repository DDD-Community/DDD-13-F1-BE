package com.f1.quiket.infra.ai.service;

import com.f1.quiket.domain.lecture.dto.LectureMaterialFile;
import com.f1.quiket.domain.lecture.service.LectureMaterialAiGateway;
import com.f1.quiket.infra.ai.client.GeminiClient;
import com.f1.quiket.infra.ai.client.GroqClient;
import com.f1.quiket.infra.ai.dto.AiBinaryData;
import com.f1.quiket.infra.ai.dto.AiCompletionRequest;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class InfraLectureMaterialAiGateway implements LectureMaterialAiGateway {

    private static final String PDF_MIME_TYPE = "application/pdf";

    private final GeminiClient geminiClient;
    private final GroqClient groqClient;

    public InfraLectureMaterialAiGateway(GeminiClient geminiClient, GroqClient groqClient) {
        this.geminiClient = geminiClient;
        this.groqClient = groqClient;
    }

    @Override
    public String analyzeImage(String systemMessage, String userMessage, List<LectureMaterialFile> imageFiles) {
        List<AiBinaryData> binaryData = imageFiles.stream()
                .map(file -> AiBinaryData.builder()
                        .mimeType(resolveMimeType(file.getContentType(), "image/jpeg"))
                        .bytes(file.getBytes())
                        .build())
                .toList();
        return geminiClient.generate(
                        AiCompletionRequest.builder()
                                .systemMessage(systemMessage)
                                .userMessage(userMessage)
                                .build(),
                        binaryData
                )
                .getContent();
    }

    @Override
    public String analyzePdf(String systemMessage, String userMessage, LectureMaterialFile pdfFile) {
        return geminiClient.generate(
                        AiCompletionRequest.builder()
                                .systemMessage(systemMessage)
                                .userMessage(userMessage)
                                .build(),
                        List.of(AiBinaryData.builder()
                                .mimeType(resolveMimeType(pdfFile.getContentType(), PDF_MIME_TYPE))
                                .bytes(pdfFile.getBytes())
                                .build())
                )
                .getContent();
    }

    @Override
    public String analyzeText(String systemMessage, String userMessage) {
        return groqClient.generate(
                        AiCompletionRequest.builder()
                                .systemMessage(systemMessage)
                                .userMessage(userMessage)
                                .build()
                )
                .getContent();
    }

    private String resolveMimeType(String contentType, String defaultType) {
        return StringUtils.hasText(contentType) ? contentType : defaultType;
    }
}

