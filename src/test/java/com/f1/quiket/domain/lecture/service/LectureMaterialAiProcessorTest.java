package com.f1.quiket.domain.lecture.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.lecture.dto.LectureMaterialAiProcessRequest;
import com.f1.quiket.domain.lecture.dto.LectureMaterialAiProcessResult;
import com.f1.quiket.domain.lecture.dto.PartSplitMethod;
import com.f1.quiket.domain.material.dto.StudyMaterialFile;
import com.f1.quiket.domain.material.dto.StudyMaterialPdfTextExtractionResult;
import com.f1.quiket.domain.material.dto.StudyMaterialUploadType;
import com.f1.quiket.domain.material.port.StudyMaterialAiGateway;
import com.f1.quiket.domain.material.port.StudyMaterialPdfTextExtractor;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class LectureMaterialAiProcessorTest {

    private StudyMaterialAiGateway studyMaterialAiGateway;
    private StudyMaterialPdfTextExtractor studyMaterialPdfTextExtractor;
    private LectureMaterialAiProcessor lectureMaterialAiProcessor;

    @BeforeEach
    void setUp() {
        studyMaterialAiGateway = Mockito.mock(StudyMaterialAiGateway.class);
        studyMaterialPdfTextExtractor = Mockito.mock(StudyMaterialPdfTextExtractor.class);
        lectureMaterialAiProcessor = new LectureMaterialAiProcessor(
                studyMaterialAiGateway,
                studyMaterialPdfTextExtractor,
                new LectureMaterialAiPromptBuilder()
        );
    }

    @Test
    void process_text_uses_groq_and_parses_parts() {
        when(studyMaterialAiGateway.generateFromText(any(), any()))
                .thenReturn("""
                        {
                          "parts": [
                            {"partNumber": 1, "name": "데이터 모델링 개념", "startLine": 1, "endLine": 2}
                          ]
                        }
                        """);
        String sourceText = """
                데이터 모델링은 현실 세계의 데이터를 추상화한다.
                엔티티와 관계를 정의한다.""";

        LectureMaterialAiProcessResult result = lectureMaterialAiProcessor.process(
                LectureMaterialAiProcessRequest.builder()
                        .uploadType(StudyMaterialUploadType.TEXT)
                        .partSplitMethod(PartSplitMethod.AUTO)
                        .chapterName("1장 데이터 모델링")
                        .text(sourceText)
                        .build()
        );

        assertThat(result.getProvider()).isEqualTo("groq");
        assertThat(result.getParts()).hasSize(1);
        assertThat(result.getParts().get(0).getName()).isEqualTo("데이터 모델링 개념");
        assertThat(result.getParts().get(0).getContent()).isEqualTo(sourceText);
        verify(studyMaterialAiGateway).generateFromText(any(), any());
        verify(studyMaterialAiGateway, never()).generateFromImages(any(), any(), any());
        verify(studyMaterialAiGateway, never()).generateFromPdf(any(), any(), any());
    }

    @Test
    void process_pdf_with_text_layer_uses_groq() {
        StudyMaterialFile pdfFile = StudyMaterialFile.builder()
                .fileName("lecture.pdf")
                .contentType("application/pdf")
                .bytes("pdf".getBytes())
                .build();
        when(studyMaterialPdfTextExtractor.extract(any()))
                .thenReturn(StudyMaterialPdfTextExtractionResult.builder()
                        .hasTextLayer(true)
                        .extractedText("텍스트 레이어 본문")
                        .build());
        when(studyMaterialAiGateway.generateFromText(any(), any()))
                .thenReturn("""
                        {"parts":[{"partNumber":1,"name":"텍스트 기반 파트","startLine":1,"endLine":1}]}
                        """);

        LectureMaterialAiProcessResult result = lectureMaterialAiProcessor.process(
                LectureMaterialAiProcessRequest.builder()
                        .uploadType(StudyMaterialUploadType.PDF)
                        .partSplitMethod(PartSplitMethod.AUTO)
                        .chapterName("1장")
                        .files(List.of(pdfFile))
                        .build()
        );

        assertThat(result.getProvider()).isEqualTo("groq");
        assertThat(result.getExtractedText()).isEqualTo("텍스트 레이어 본문");
        assertThat(result.getParts().get(0).getContent()).isEqualTo("텍스트 레이어 본문");
        verify(studyMaterialAiGateway).generateFromText(any(), any());
        verify(studyMaterialAiGateway, never()).generateFromPdf(any(), any(), any());
    }

    @Test
    void process_text_splits_original_text_by_groq_line_ranges() {
        when(studyMaterialAiGateway.generateFromText(any(), any()))
                .thenReturn("""
                        {"parts":[
                          {"partNumber":1,"name":"개념","startLine":1,"endLine":2},
                          {"partNumber":2,"name":"예시","startLine":3,"endLine":3}
                        ]}
                        """);
        String sourceText = """
                개념 1
                개념 2
                예시 1""";

        LectureMaterialAiProcessResult result = lectureMaterialAiProcessor.process(
                LectureMaterialAiProcessRequest.builder()
                        .uploadType(StudyMaterialUploadType.TEXT)
                        .partSplitMethod(PartSplitMethod.AUTO)
                        .chapterName("1장")
                        .text(sourceText)
                        .build()
        );

        assertThat(result.getParts()).extracting("content")
                .containsExactly("개념 1\n개념 2\n", "예시 1");
    }

    @Test
    void process_text_adds_unclassified_part_when_groq_middle_range_is_missing() {
        when(studyMaterialAiGateway.generateFromText(any(), any()))
                .thenReturn("""
                        {"parts":[
                          {"partNumber":1,"name":"개념","startLine":1,"endLine":1},
                          {"partNumber":2,"name":"예시","startLine":3,"endLine":3}
                        ]}
                        """);
        String sourceText = """
                개념
                누락
                예시""";

        LectureMaterialAiProcessResult result = lectureMaterialAiProcessor.process(
                LectureMaterialAiProcessRequest.builder()
                        .uploadType(StudyMaterialUploadType.TEXT)
                        .partSplitMethod(PartSplitMethod.AUTO)
                        .chapterName("1장")
                        .text(sourceText)
                        .build()
        );

        assertThat(result.getParts()).extracting("name")
                .containsExactly("개념", "미분류", "예시");
        assertThat(result.getParts()).extracting("content")
                .containsExactly("개념\n", "누락\n", "예시");
    }

    @Test
    void process_text_adds_unclassified_part_when_groq_tail_range_is_missing() {
        when(studyMaterialAiGateway.generateFromText(any(), any()))
                .thenReturn("""
                        {"parts":[{"partNumber":1,"name":"개념","startLine":1,"endLine":1}]}
                        """);
        String sourceText = """
                개념
                마지막""";

        LectureMaterialAiProcessResult result = lectureMaterialAiProcessor.process(
                LectureMaterialAiProcessRequest.builder()
                        .uploadType(StudyMaterialUploadType.TEXT)
                        .partSplitMethod(PartSplitMethod.AUTO)
                        .chapterName("1장")
                        .text(sourceText)
                        .build()
        );

        assertThat(result.getParts()).extracting("name")
                .containsExactly("개념", "미분류");
        assertThat(result.getParts()).extracting("content")
                .containsExactly("개념\n", "마지막");
    }

    @Test
    void process_pdf_without_text_layer_uses_gemini() {
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
                .thenReturn("""
                        {"parts":[{"partNumber":1,"partName":"OCR 파트","content":"OCR 본문"}]}
                        """);

        LectureMaterialAiProcessResult result = lectureMaterialAiProcessor.process(
                LectureMaterialAiProcessRequest.builder()
                        .uploadType(StudyMaterialUploadType.PDF)
                        .partSplitMethod(PartSplitMethod.AUTO)
                        .chapterName("1장")
                        .files(List.of(pdfFile))
                        .build()
        );

        assertThat(result.getProvider()).isEqualTo("gemini");
        assertThat(result.getParts().get(0).getName()).isEqualTo("OCR 파트");
        verify(studyMaterialAiGateway).generateFromPdf(any(), any(), any());
        verify(studyMaterialAiGateway, never()).generateFromText(any(), any());
    }

    @Test
    void process_image_parses_fenced_json_response() {
        StudyMaterialFile imageFile = StudyMaterialFile.builder()
                .fileName("slide.png")
                .contentType("image/png")
                .bytes("image".getBytes())
                .build();
        when(studyMaterialAiGateway.generateFromImages(any(), any(), any()))
                .thenReturn("""
                        ```json
                        {"parts":[{"partNumber":1,"name":"이미지 파트","content":"이미지 OCR 본문"}]}
                        ```
                        """);

        LectureMaterialAiProcessResult result = lectureMaterialAiProcessor.process(
                LectureMaterialAiProcessRequest.builder()
                        .uploadType(StudyMaterialUploadType.IMAGE)
                        .partSplitMethod(PartSplitMethod.AUTO)
                        .chapterName("1장")
                        .files(List.of(imageFile))
                        .build()
        );

        assertThat(result.getProvider()).isEqualTo("gemini");
        assertThat(result.getParts().get(0).getContent()).isEqualTo("이미지 OCR 본문");
        verify(studyMaterialAiGateway).generateFromImages(any(), any(), any());
    }
}
