package com.f1.quiket.infra.material.client;

import com.f1.quiket.domain.material.dto.StudyMaterialFile;
import com.f1.quiket.domain.material.port.StudyMaterialAiGateway;
import com.f1.quiket.infra.gemini.client.GeminiClient;
import com.f1.quiket.infra.gemini.dto.GeminiBinaryData;
import com.f1.quiket.infra.gemini.dto.GeminiCompletionRequest;
import com.f1.quiket.infra.groq.client.GroqClient;
import com.f1.quiket.infra.groq.dto.GroqCompletionRequest;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class InfraStudyMaterialAiGateway implements StudyMaterialAiGateway {

    private static final String PDF_MIME_TYPE = "application/pdf";

    private final GeminiClient geminiClient;
    private final GroqClient groqClient;

    public InfraStudyMaterialAiGateway(GeminiClient geminiClient, GroqClient groqClient) {
        this.geminiClient = geminiClient;
        this.groqClient = groqClient;
    }

    @Override
    public String generateFromImages(String systemMessage, String userMessage, List<StudyMaterialFile> imageFiles) {
        List<GeminiBinaryData> binaryData = imageFiles.stream()
                .map(file -> GeminiBinaryData.builder()
                        .mimeType(resolveMimeType(file.getContentType(), "image/jpeg"))
                        .bytes(file.getBytes())
                        .build())
                .toList();
        return geminiClient.generate(
                        GeminiCompletionRequest.builder()
                                .systemMessage(systemMessage)
                                .userMessage(userMessage)
                                .build(),
                        binaryData
                )
                .getContent();
    }

    @Override
    public String generateFromPdf(String systemMessage, String userMessage, StudyMaterialFile pdfFile) {
        return geminiClient.generate(
                        GeminiCompletionRequest.builder()
                                .systemMessage(systemMessage)
                                .userMessage(userMessage)
                                .build(),
                        List.of(GeminiBinaryData.builder()
                                .mimeType(resolveMimeType(pdfFile.getContentType(), PDF_MIME_TYPE))
                                .bytes(pdfFile.getBytes())
                                .build())
                )
                .getContent();
    }

    @Override
    public String generateFromText(String systemMessage, String userMessage) {
        return groqClient.generate(
                        GroqCompletionRequest.builder()
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

