package com.f1.quiket.domain.lecture.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.f1.quiket.domain.lecture.dto.LectureMaterialAiProcessRequest;
import com.f1.quiket.domain.lecture.dto.LectureMaterialAiProcessResult;
import com.f1.quiket.domain.lecture.dto.PartSplitMethod;
import com.f1.quiket.domain.material.dto.StudyMaterialFile;
import com.f1.quiket.domain.material.dto.StudyMaterialUploadType;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StringUtils;

/**
 * 강의 자료 AI 실연동 수동 통합 테스트
 *
 * src/test/resources/test-materials 파일 기반 업로드 시뮬레이션
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("manual")
@Slf4j
class LectureMaterialAiProcessorRealIntegrationTest {

    private static final String ENABLED_ENV = "AI_REAL_MATERIAL_TEST_ENABLED";
    private static final String GEMINI_API_KEY_ENV = "GEMINI_API_KEY";
    private static final String GROQ_API_KEY_ENV = "GROQ_API_KEY";
    private static final Path TEST_MATERIALS_PATH = Path.of("src/test/resources/test-materials");
    private static final Path RESULT_OUTPUT_PATH = Path.of("build/ai-material-test-results");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private LectureMaterialAiProcessor lectureMaterialAiProcessor;

    @Test
    void process_text_layer_pdf_file_upload_classifies_parts_with_groq() {
        // 수동 실행 플래그와 Groq API 키 확인
        requireEnabled();
        requireEnv(GROQ_API_KEY_ENV);

        // 텍스트 레이어 PDF 업로드 시뮬레이션
        StudyMaterialFile file = uploadFile("sample-text-layer.pdf", "application/pdf");

        // Tika 텍스트 레이어 판별 후 Groq 파트 분류
        LectureMaterialAiProcessResult result = lectureMaterialAiProcessor.process(
                LectureMaterialAiProcessRequest.builder()
                        .uploadType(StudyMaterialUploadType.PDF)
                        .partSplitMethod(PartSplitMethod.AUTO)
                        .chapterName("텍스트 레이어 PDF 테스트")
                        .files(List.of(file))
                        .build()
        );

        // 텍스트 레이어 추출 결과와 파트 분류 결과 검증
        assertThat(result.getExtractedText()).isNotBlank();
        assertPartsGenerated(result, "groq");
        writeResult("text-layer-pdf", result);
    }

    @Test
    void process_image_layer_pdf_file_upload_classifies_parts_with_gemini() {
        // 수동 실행 플래그와 Gemini API 키 확인
        requireEnabled();
        requireEnv(GEMINI_API_KEY_ENV);

        // 이미지 레이어 PDF 업로드 시뮬레이션
        StudyMaterialFile file = uploadFile("sample-image-layer.pdf", "application/pdf");

        // 텍스트 레이어 없음 판별 후 Gemini OCR 및 파트 분류
        LectureMaterialAiProcessResult result = lectureMaterialAiProcessor.process(
                LectureMaterialAiProcessRequest.builder()
                        .uploadType(StudyMaterialUploadType.PDF)
                        .partSplitMethod(PartSplitMethod.AUTO)
                        .chapterName("이미지 레이어 PDF 테스트")
                        .files(List.of(file))
                        .build()
        );

        assertPartsGenerated(result, "gemini");
        writeResult("image-layer-pdf", result);
    }

    @Test
    void process_txt_file_upload_classifies_parts_with_groq() {
        // 수동 실행 플래그와 Groq API 키 확인
        requireEnabled();
        requireEnv(GROQ_API_KEY_ENV);

        // TXT 파일 업로드 입력 텍스트 시뮬레이션
        String text = readTextFile("sample-text.txt");

        // 직접 입력 텍스트 기반 Groq 파트 분류
        LectureMaterialAiProcessResult result = lectureMaterialAiProcessor.process(
                LectureMaterialAiProcessRequest.builder()
                        .uploadType(StudyMaterialUploadType.TEXT)
                        .partSplitMethod(PartSplitMethod.AUTO)
                        .chapterName("TXT 파일 테스트")
                        .text(text)
                        .build()
        );

        assertPartsGenerated(result, "groq");
        writeResult("txt-file", result);
    }

    @Test
    void process_image_file_upload_classifies_parts_with_gemini() {
        // 수동 실행 플래그와 Gemini API 키 확인
        requireEnabled();
        requireEnv(GEMINI_API_KEY_ENV);

        // 이미지 파일 업로드 시뮬레이션
        StudyMaterialFile file = uploadFile("sample-image.png", "image/png");

        // Gemini 이미지 OCR 및 파트 분류
        LectureMaterialAiProcessResult result = lectureMaterialAiProcessor.process(
                LectureMaterialAiProcessRequest.builder()
                        .uploadType(StudyMaterialUploadType.IMAGE)
                        .partSplitMethod(PartSplitMethod.AUTO)
                        .chapterName("이미지 파일 테스트")
                        .files(List.of(file))
                        .build()
        );

        assertPartsGenerated(result, "gemini");
        writeResult("image-file", result);
    }

    private void requireEnabled() {
        String enabled = System.getenv(ENABLED_ENV);
        if (!"true".equalsIgnoreCase(enabled)) {
            progress("skipped: set " + ENABLED_ENV + "=true to run real AI material test");
        }
        assumeTrue(
                "true".equalsIgnoreCase(enabled),
                () -> "[AI MATERIAL REAL TEST] skipped: set " + ENABLED_ENV + "=true"
        );
    }

    private void requireEnv(String envName) {
        String value = System.getenv(envName);
        if (!StringUtils.hasText(value)) {
            progress("skipped: missing env " + envName);
        }
        assumeTrue(StringUtils.hasText(value), () -> "[AI MATERIAL REAL TEST] skipped: missing env " + envName);
    }

    private StudyMaterialFile uploadFile(String fileName, String contentType) {
        Path path = TEST_MATERIALS_PATH.resolve(fileName);
        try {
            assertThat(Files.exists(path)).as("sample file exists: " + path).isTrue();
            return StudyMaterialFile.builder()
                    .fileName(fileName)
                    .contentType(contentType)
                    .bytes(Files.readAllBytes(path))
                    .build();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String readTextFile(String fileName) {
        Path path = TEST_MATERIALS_PATH.resolve(fileName);
        try {
            assertThat(Files.exists(path)).as("sample file exists: " + path).isTrue();
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void assertPartsGenerated(LectureMaterialAiProcessResult result, String expectedProvider) {
        progress("provider=" + result.getProvider() + ", parts=" + result.getParts().size());
        assertThat(result.getProvider()).isEqualTo(expectedProvider);
        assertThat(result.getParts()).isNotEmpty();
        assertThat(result.getParts())
                .allSatisfy(part -> {
                    assertThat(part.getPartNumber()).isNotNull();
                    assertThat(part.getName()).isNotBlank();
                    assertThat(part.getContent()).isNotBlank();
                });
    }

    private void writeResult(String caseName, LectureMaterialAiProcessResult result) {
        try {
            Files.createDirectories(RESULT_OUTPUT_PATH);
            Path outputPath = RESULT_OUTPUT_PATH.resolve(caseName + "-result.json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputPath.toFile(), result);
            progress("result written: " + outputPath);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void progress(String message) {
        String fullMessage = "[AI MATERIAL REAL TEST] " + message;
        log.info(fullMessage);
        System.out.println(fullMessage);
    }
}
