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

/**
 * 학습 자료 AI 처리 포트 구현체
 *
 * 도메인 요청을 Gemini와 Groq 클라이언트 호출로 변환
 */
@Component
public class InfraStudyMaterialAiGateway implements StudyMaterialAiGateway {

    private static final String PDF_MIME_TYPE = "application/pdf";

    private final GeminiClient geminiClient;
    private final GroqClient groqClient;

    public InfraStudyMaterialAiGateway(GeminiClient geminiClient, GroqClient groqClient) {
        this.geminiClient = geminiClient;
        this.groqClient = groqClient;
    }

    /**
     * 이미지 파일 기반 AI 텍스트 생성 위임
     */
    @Override
    public String generateFromImages(String systemMessage, String userMessage, List<StudyMaterialFile> imageFiles) {
        // Gemini inlineData 요청 데이터 변환
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

    /**
     * PDF 파일 기반 AI 텍스트 생성 위임
     */
    @Override
    public String generateFromPdf(String systemMessage, String userMessage, StudyMaterialFile pdfFile) {
        // Gemini PDF inlineData 요청 데이터 변환
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

    /**
     * 텍스트 기반 AI 텍스트 생성 위임
     */
    @Override
    public String generateFromText(String systemMessage, String userMessage) {
        // Groq 텍스트 요청 위임
        return groqClient.generate(
                        GroqCompletionRequest.builder()
                                .systemMessage(systemMessage)
                                .userMessage(userMessage)
                                .build()
                )
                .getContent();
    }

    /**
     * 콘텐츠 타입 기본값 보정
     */
    private String resolveMimeType(String contentType, String defaultType) {
        return StringUtils.hasText(contentType) ? contentType : defaultType;
    }
}

