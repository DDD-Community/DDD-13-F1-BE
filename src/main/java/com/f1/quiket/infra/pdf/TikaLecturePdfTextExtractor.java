package com.f1.quiket.infra.pdf;

import com.f1.quiket.domain.lecture.dto.LectureMaterialFile;
import com.f1.quiket.domain.lecture.dto.LecturePdfTextExtractionResult;
import com.f1.quiket.domain.lecture.service.LecturePdfTextExtractor;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TikaLecturePdfTextExtractor implements LecturePdfTextExtractor {

    private final Tika tika = new Tika();

    @Override
    public LecturePdfTextExtractionResult extract(LectureMaterialFile pdfFile) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(pdfFile.getBytes())) {
            String extractedText = tika.parseToString(inputStream);
            String normalized = extractedText == null ? "" : extractedText.trim();
            boolean hasTextLayer = hasMeaningfulText(normalized);
            return LecturePdfTextExtractionResult.builder()
                    .hasTextLayer(hasTextLayer)
                    .extractedText(normalized)
                    .build();
        } catch (IOException | TikaException e) {
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, "PDF 텍스트 추출에 실패했습니다.", e);
        }
    }

    private boolean hasMeaningfulText(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        return text.chars().anyMatch(ch -> Character.isLetterOrDigit(ch));
    }
}
