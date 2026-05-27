package com.f1.quiket.domain.material.processor;

import com.f1.quiket.domain.material.dto.StudyMaterialFile;
import com.f1.quiket.domain.material.dto.StudyMaterialPdfTextExtractionResult;
import com.f1.quiket.domain.material.dto.StudyMaterialTextExtractionRequest;
import com.f1.quiket.domain.material.dto.StudyMaterialTextExtractionResult;
import com.f1.quiket.domain.material.dto.StudyMaterialUploadType;
import com.f1.quiket.domain.material.port.StudyMaterialAiGateway;
import com.f1.quiket.domain.material.port.StudyMaterialPdfTextExtractor;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 학습 자료 텍스트 추출 오케스트레이터
 */
@Component
@RequiredArgsConstructor
public class StudyMaterialTextExtractor {

    private static final String OCR_SYSTEM_MESSAGE = """
            너는 Quiket 학습 자료 OCR 엔진이다.
            반드시 추출된 텍스트만 반환한다.
            설명, 마크다운, 코드블록은 절대 포함하지 않는다.
            """;

    private static final String OCR_USER_MESSAGE = """
            업로드 파일의 모든 학습 텍스트를 읽기 순서대로 추출한다.
            표와 목록은 의미가 유지되도록 줄바꿈으로 정리한다.
            파트 분류, 제목 생성, 요약은 수행하지 않는다.
            """;

    private final StudyMaterialAiGateway studyMaterialAiGateway;
    private final StudyMaterialPdfTextExtractor studyMaterialPdfTextExtractor;

    public StudyMaterialTextExtractionResult extract(StudyMaterialTextExtractionRequest request) {
        validateRequest(request);

        return switch (request.getUploadType()) {
            case TEXT -> extractText(request);
            case IMAGE -> extractImage(request);
            case PDF -> extractPdf(request);
        };
    }

    private StudyMaterialTextExtractionResult extractText(StudyMaterialTextExtractionRequest request) {
        return StudyMaterialTextExtractionResult.builder()
                .provider("none")
                .extractedText(request.getText())
                .build();
    }

    private StudyMaterialTextExtractionResult extractImage(StudyMaterialTextExtractionRequest request) {
        String extractedText = studyMaterialAiGateway.generateFromImages(
                OCR_SYSTEM_MESSAGE,
                OCR_USER_MESSAGE,
                request.getFiles()
        );
        return StudyMaterialTextExtractionResult.builder()
                .provider("gemini")
                .extractedText(extractedText.trim())
                .build();
    }

    private StudyMaterialTextExtractionResult extractPdf(StudyMaterialTextExtractionRequest request) {
        StudyMaterialFile pdfFile = request.getFiles().get(0);
        StudyMaterialPdfTextExtractionResult extractionResult = studyMaterialPdfTextExtractor.extract(pdfFile);
        if (extractionResult.isHasTextLayer()) {
            return StudyMaterialTextExtractionResult.builder()
                    .provider("tika")
                    .extractedText(extractionResult.getExtractedText())
                    .build();
        }

        String extractedText = studyMaterialAiGateway.generateFromPdf(
                OCR_SYSTEM_MESSAGE,
                OCR_USER_MESSAGE,
                pdfFile
        );
        return StudyMaterialTextExtractionResult.builder()
                .provider("gemini")
                .extractedText(extractedText.trim())
                .build();
    }

    private void validateRequest(StudyMaterialTextExtractionRequest request) {
        if (request == null || request.getUploadType() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "학습 자료 텍스트 추출 요청값이 올바르지 않습니다.");
        }
        if (request.getUploadType() == StudyMaterialUploadType.TEXT && !StringUtils.hasText(request.getText())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "텍스트 입력은 본문 텍스트가 필요합니다.");
        }
        if (request.getUploadType() != StudyMaterialUploadType.TEXT
                && (request.getFiles() == null || request.getFiles().isEmpty())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "파일 입력은 최소 1개 파일이 필요합니다.");
        }
    }
}
