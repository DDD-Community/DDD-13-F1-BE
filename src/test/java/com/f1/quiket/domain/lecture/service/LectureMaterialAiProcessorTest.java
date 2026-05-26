package com.f1.quiket.domain.lecture.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.lecture.dto.LectureMaterialAiProcessRequest;
import com.f1.quiket.domain.lecture.dto.LectureMaterialAiProcessResult;
import com.f1.quiket.domain.lecture.dto.LectureMaterialFile;
import com.f1.quiket.domain.lecture.dto.LecturePdfTextExtractionResult;
import com.f1.quiket.domain.lecture.dto.LectureUploadType;
import com.f1.quiket.domain.lecture.dto.PartSplitMethod;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class LectureMaterialAiProcessorTest {

    private LectureMaterialAiGateway lectureMaterialAiGateway;
    private LecturePdfTextExtractor lecturePdfTextExtractor;
    private LectureMaterialAiProcessor lectureMaterialAiProcessor;

    @BeforeEach
    void setUp() {
        lectureMaterialAiGateway = Mockito.mock(LectureMaterialAiGateway.class);
        lecturePdfTextExtractor = Mockito.mock(LecturePdfTextExtractor.class);
        lectureMaterialAiProcessor = new LectureMaterialAiProcessor(
                lectureMaterialAiGateway,
                lecturePdfTextExtractor
        );
    }

    @Test
    void process_text_uses_groq_and_parses_parts() {
        when(lectureMaterialAiGateway.analyzeText(any(), any()))
                .thenReturn("""
                        {
                          "parts": [
                            {"partNumber": 1, "name": "데이터 모델링 개념", "content": "데이터 모델링 기본 개념 정리"}
                          ]
                        }
                        """);

        LectureMaterialAiProcessResult result = lectureMaterialAiProcessor.process(
                LectureMaterialAiProcessRequest.builder()
                        .uploadType(LectureUploadType.TEXT)
                        .partSplitMethod(PartSplitMethod.AUTO)
                        .chapterName("1장 데이터 모델링")
                        .text("데이터 모델링은 현실 세계의 데이터를 추상화한다.")
                        .build()
        );

        assertThat(result.getProvider()).isEqualTo("groq");
        assertThat(result.getParts()).hasSize(1);
        assertThat(result.getParts().get(0).getName()).isEqualTo("데이터 모델링 개념");
        verify(lectureMaterialAiGateway).analyzeText(any(), any());
        verify(lectureMaterialAiGateway, never()).analyzeImage(any(), any(), any());
        verify(lectureMaterialAiGateway, never()).analyzePdf(any(), any(), any());
    }

    @Test
    void process_pdf_with_text_layer_uses_groq() {
        LectureMaterialFile pdfFile = LectureMaterialFile.builder()
                .fileName("lecture.pdf")
                .contentType("application/pdf")
                .bytes("pdf".getBytes())
                .build();
        when(lecturePdfTextExtractor.extract(any()))
                .thenReturn(LecturePdfTextExtractionResult.builder()
                        .hasTextLayer(true)
                        .extractedText("텍스트 레이어 본문")
                        .build());
        when(lectureMaterialAiGateway.analyzeText(any(), any()))
                .thenReturn("""
                        {"parts":[{"partNumber":1,"name":"텍스트 기반 파트","content":"본문"}]}
                        """);

        LectureMaterialAiProcessResult result = lectureMaterialAiProcessor.process(
                LectureMaterialAiProcessRequest.builder()
                        .uploadType(LectureUploadType.PDF)
                        .partSplitMethod(PartSplitMethod.AUTO)
                        .chapterName("1장")
                        .files(List.of(pdfFile))
                        .build()
        );

        assertThat(result.getProvider()).isEqualTo("groq");
        assertThat(result.getExtractedText()).isEqualTo("텍스트 레이어 본문");
        verify(lectureMaterialAiGateway).analyzeText(any(), any());
        verify(lectureMaterialAiGateway, never()).analyzePdf(any(), any(), any());
    }

    @Test
    void process_pdf_without_text_layer_uses_gemini() {
        LectureMaterialFile pdfFile = LectureMaterialFile.builder()
                .fileName("scan.pdf")
                .contentType("application/pdf")
                .bytes("pdf".getBytes())
                .build();
        when(lecturePdfTextExtractor.extract(any()))
                .thenReturn(LecturePdfTextExtractionResult.builder()
                        .hasTextLayer(false)
                        .extractedText("")
                        .build());
        when(lectureMaterialAiGateway.analyzePdf(any(), any(), any()))
                .thenReturn("""
                        {"parts":[{"partNumber":1,"partName":"OCR 파트","content":"OCR 본문"}]}
                        """);

        LectureMaterialAiProcessResult result = lectureMaterialAiProcessor.process(
                LectureMaterialAiProcessRequest.builder()
                        .uploadType(LectureUploadType.PDF)
                        .partSplitMethod(PartSplitMethod.AUTO)
                        .chapterName("1장")
                        .files(List.of(pdfFile))
                        .build()
        );

        assertThat(result.getProvider()).isEqualTo("gemini");
        assertThat(result.getParts().get(0).getName()).isEqualTo("OCR 파트");
        verify(lectureMaterialAiGateway).analyzePdf(any(), any(), any());
        verify(lectureMaterialAiGateway, never()).analyzeText(any(), any());
    }

    @Test
    void process_image_parses_fenced_json_response() {
        LectureMaterialFile imageFile = LectureMaterialFile.builder()
                .fileName("slide.png")
                .contentType("image/png")
                .bytes("image".getBytes())
                .build();
        when(lectureMaterialAiGateway.analyzeImage(any(), any(), any()))
                .thenReturn("""
                        ```json
                        {"parts":[{"partNumber":1,"name":"이미지 파트","content":"이미지 OCR 본문"}]}
                        ```
                        """);

        LectureMaterialAiProcessResult result = lectureMaterialAiProcessor.process(
                LectureMaterialAiProcessRequest.builder()
                        .uploadType(LectureUploadType.IMAGE)
                        .partSplitMethod(PartSplitMethod.AUTO)
                        .chapterName("1장")
                        .files(List.of(imageFile))
                        .build()
        );

        assertThat(result.getProvider()).isEqualTo("gemini");
        assertThat(result.getParts().get(0).getContent()).isEqualTo("이미지 OCR 본문");
        verify(lectureMaterialAiGateway).analyzeImage(any(), any(), any());
    }
}
