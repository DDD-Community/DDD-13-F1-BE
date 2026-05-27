package com.f1.quiket.domain.material.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.material.dto.StudyMaterialFile;
import com.f1.quiket.domain.material.dto.StudyMaterialPdfTextExtractionResult;
import com.f1.quiket.domain.material.dto.StudyMaterialTextExtractionRequest;
import com.f1.quiket.domain.material.dto.StudyMaterialTextExtractionResult;
import com.f1.quiket.domain.material.dto.StudyMaterialUploadType;
import com.f1.quiket.domain.material.port.StudyMaterialAiGateway;
import com.f1.quiket.domain.material.port.StudyMaterialPdfTextExtractor;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class StudyMaterialTextExtractorTest {

    private StudyMaterialAiGateway studyMaterialAiGateway;
    private StudyMaterialPdfTextExtractor studyMaterialPdfTextExtractor;
    private StudyMaterialTextExtractor studyMaterialTextExtractor;

    @BeforeEach
    void setUp() {
        studyMaterialAiGateway = Mockito.mock(StudyMaterialAiGateway.class);
        studyMaterialPdfTextExtractor = Mockito.mock(StudyMaterialPdfTextExtractor.class);
        studyMaterialTextExtractor = new StudyMaterialTextExtractor(
                studyMaterialAiGateway,
                studyMaterialPdfTextExtractor,
                new StudyMaterialTextExtractionPromptBuilder()
        );
    }

    @Test
    void extract_text_returns_input_without_ai_call() {
        StudyMaterialTextExtractionResult result = studyMaterialTextExtractor.extract(
                StudyMaterialTextExtractionRequest.builder()
                        .uploadType(StudyMaterialUploadType.TEXT)
                        .text("직접 입력한 파트 본문")
                        .build()
        );

        assertThat(result.getProvider()).isEqualTo("none");
        assertThat(result.getExtractedText()).isEqualTo("직접 입력한 파트 본문");
        verify(studyMaterialAiGateway, never()).generateFromText(any(), any());
        verify(studyMaterialAiGateway, never()).generateFromImages(any(), any(), any());
        verify(studyMaterialAiGateway, never()).generateFromPdf(any(), any(), any());
    }

    @Test
    void extract_pdf_with_text_layer_uses_tika_text_without_ai_call() {
        StudyMaterialFile pdfFile = StudyMaterialFile.builder()
                .fileName("lecture.pdf")
                .contentType("application/pdf")
                .bytes("pdf".getBytes())
                .build();
        when(studyMaterialPdfTextExtractor.extract(any()))
                .thenReturn(StudyMaterialPdfTextExtractionResult.builder()
                        .hasTextLayer(true)
                        .extractedText("PDF 텍스트 레이어 본문")
                        .build());

        StudyMaterialTextExtractionResult result = studyMaterialTextExtractor.extract(
                StudyMaterialTextExtractionRequest.builder()
                        .uploadType(StudyMaterialUploadType.PDF)
                        .files(List.of(pdfFile))
                        .build()
        );

        assertThat(result.getProvider()).isEqualTo("tika");
        assertThat(result.getExtractedText()).isEqualTo("PDF 텍스트 레이어 본문");
        verify(studyMaterialAiGateway, never()).generateFromPdf(any(), any(), any());
    }

    @Test
    void extract_pdf_without_text_layer_uses_gemini_ocr_only() {
        StudyMaterialFile pdfFile = StudyMaterialFile.builder()
                .fileName("scan.pdf")
                .contentType("application/pdf")
                .bytes("pdf".getBytes())
                .build();
        when(studyMaterialPdfTextExtractor.extract(any()))
                .thenReturn(StudyMaterialPdfTextExtractionResult.builder()
                        .hasTextLayer(false)
                        .extractedText("")
                        .build());
        when(studyMaterialAiGateway.generateFromPdf(any(), any(), any()))
                .thenReturn("스캔 PDF OCR 본문");

        StudyMaterialTextExtractionResult result = studyMaterialTextExtractor.extract(
                StudyMaterialTextExtractionRequest.builder()
                        .uploadType(StudyMaterialUploadType.PDF)
                        .files(List.of(pdfFile))
                        .build()
        );

        assertThat(result.getProvider()).isEqualTo("gemini");
        assertThat(result.getExtractedText()).isEqualTo("스캔 PDF OCR 본문");
        verify(studyMaterialAiGateway).generateFromPdf(any(), any(), any());
    }

    @Test
    void extract_image_uses_gemini_ocr_only() {
        StudyMaterialFile imageFile = StudyMaterialFile.builder()
                .fileName("slide.png")
                .contentType("image/png")
                .bytes("image".getBytes())
                .build();
        when(studyMaterialAiGateway.generateFromImages(any(), any(), any()))
                .thenReturn("이미지 OCR 본문");

        StudyMaterialTextExtractionResult result = studyMaterialTextExtractor.extract(
                StudyMaterialTextExtractionRequest.builder()
                        .uploadType(StudyMaterialUploadType.IMAGE)
                        .files(List.of(imageFile))
                        .build()
        );

        assertThat(result.getProvider()).isEqualTo("gemini");
        assertThat(result.getExtractedText()).isEqualTo("이미지 OCR 본문");
        verify(studyMaterialAiGateway).generateFromImages(any(), any(), any());
    }
}
