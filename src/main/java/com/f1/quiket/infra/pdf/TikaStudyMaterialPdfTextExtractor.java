package com.f1.quiket.infra.pdf;

import com.f1.quiket.domain.material.dto.StudyMaterialFile;
import com.f1.quiket.domain.material.dto.StudyMaterialPdfTextExtractionResult;
import com.f1.quiket.domain.material.port.StudyMaterialPdfTextExtractor;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Tika 기반 PDF 텍스트 추출기
 *
 * PDF 텍스트 레이어 존재 여부 판별
 */
@Component
public class TikaStudyMaterialPdfTextExtractor implements StudyMaterialPdfTextExtractor {

    private final Tika tika = new Tika();

    /**
     * Tika 기반 PDF 텍스트 추출
     */
    @Override
    public StudyMaterialPdfTextExtractionResult extract(StudyMaterialFile pdfFile) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(pdfFile.getBytes())) {
            String extractedText = tika.parseToString(inputStream);
            String normalized = extractedText == null ? "" : extractedText.trim();
            // 추출 텍스트 기반 텍스트 레이어 판별
            boolean hasTextLayer = hasMeaningfulText(normalized);
            return StudyMaterialPdfTextExtractionResult.builder()
                    .hasTextLayer(hasTextLayer)
                    .extractedText(normalized)
                    .build();
        } catch (IOException | TikaException e) {
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, "PDF 텍스트 추출에 실패했습니다.", e);
        }
    }

    /**
     * 의미 있는 텍스트 존재 여부
     */
    private boolean hasMeaningfulText(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        return text.chars().anyMatch(ch -> Character.isLetterOrDigit(ch));
    }
}
