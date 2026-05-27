package com.f1.quiket.domain.lecture.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.f1.quiket.domain.lecture.dto.LectureMaterialAiProcessRequest;
import com.f1.quiket.domain.lecture.dto.LectureMaterialAiProcessResult;
import com.f1.quiket.domain.lecture.dto.LecturePartDraft;
import com.f1.quiket.domain.lecture.dto.LecturePartSplitPlan;
import com.f1.quiket.domain.material.dto.StudyMaterialFile;
import com.f1.quiket.domain.material.dto.StudyMaterialPdfTextExtractionResult;
import com.f1.quiket.domain.material.dto.StudyMaterialUploadType;
import com.f1.quiket.domain.material.port.StudyMaterialAiGateway;
import com.f1.quiket.domain.material.port.StudyMaterialPdfTextExtractor;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 강의 자료 AI 처리 오케스트레이터
 *
 * 강의 업로드 자료의 OCR, 텍스트 추출, 파트 분류 흐름 조합
 */
@Component
@RequiredArgsConstructor
public class LectureMaterialAiProcessor {

    private static final String SYSTEM_MESSAGE = """
            너는 Quiket 강의 자료 파트 분류 엔진이다.
            반드시 JSON만 반환한다.
            코드블록, 마크다운, 설명 문장은 절대 포함하지 않는다.
            """;

    private final StudyMaterialAiGateway studyMaterialAiGateway;
    private final StudyMaterialPdfTextExtractor studyMaterialPdfTextExtractor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 업로드 타입별 강의 자료 AI 처리
     */
    public LectureMaterialAiProcessResult process(LectureMaterialAiProcessRequest request) {
        validateRequest(request);

        // 업로드 타입별 AI 처리 분기
        return switch (request.getUploadType()) {
            case IMAGE -> processImage(request);
            case PDF -> processPdf(request);
            case TEXT -> processText(request);
        };
    }

    /**
     * 이미지 OCR 및 파트 분류 처리
     */
    private LectureMaterialAiProcessResult processImage(LectureMaterialAiProcessRequest request) {
        // Gemini 기반 이미지 OCR 및 파트 분류
        String aiResponse = studyMaterialAiGateway.generateFromImages(
                SYSTEM_MESSAGE,
                buildGeminiPrompt(request, null),
                request.getFiles()
        );
        List<LecturePartDraft> parts = parseParts(aiResponse);
        return LectureMaterialAiProcessResult.builder()
                .provider("gemini")
                .extractedText(null)
                .parts(parts)
                .build();
    }

    /**
     * PDF 텍스트 레이어 판별 후 AI 처리
     */
    private LectureMaterialAiProcessResult processPdf(LectureMaterialAiProcessRequest request) {
        StudyMaterialFile pdfFile = request.getFiles().get(0);
        StudyMaterialPdfTextExtractionResult extractionResult = studyMaterialPdfTextExtractor.extract(pdfFile);

        // 텍스트 레이어 PDF는 추출 텍스트 기반 Groq 분류
        if (extractionResult.isHasTextLayer()) {
            String text = extractionResult.getExtractedText();
            String aiResponse = studyMaterialAiGateway.generateFromText(
                    SYSTEM_MESSAGE,
                    buildGroqPrompt(request, text)
            );
            return LectureMaterialAiProcessResult.builder()
                    .provider("groq")
                    .extractedText(text)
                    .parts(parseParts(aiResponse))
                    .build();
        }

        // 스캔 PDF는 Gemini OCR 및 파트 분류
        String aiResponse = studyMaterialAiGateway.generateFromPdf(
                SYSTEM_MESSAGE,
                buildGeminiPrompt(request, null),
                pdfFile
        );
        return LectureMaterialAiProcessResult.builder()
                .provider("gemini")
                .extractedText(extractionResult.getExtractedText())
                .parts(parseParts(aiResponse))
                .build();
    }

    /**
     * 직접 입력 텍스트 기반 파트 분류 처리
     */
    private LectureMaterialAiProcessResult processText(LectureMaterialAiProcessRequest request) {
        // 입력 텍스트 기반 Groq 파트 분류
        String aiResponse = studyMaterialAiGateway.generateFromText(
                SYSTEM_MESSAGE,
                buildGroqPrompt(request, request.getText())
        );
        return LectureMaterialAiProcessResult.builder()
                .provider("groq")
                .extractedText(request.getText())
                .parts(parseParts(aiResponse))
                .build();
    }

    /**
     * AI 처리 요청값 검증
     */
    private void validateRequest(LectureMaterialAiProcessRequest request) {
        if (request == null || request.getUploadType() == null || request.getPartSplitMethod() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "강의 자료 AI 처리 요청값이 올바르지 않습니다.");
        }
        if (request.getUploadType() == StudyMaterialUploadType.TEXT && !StringUtils.hasText(request.getText())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "텍스트 업로드는 본문 텍스트가 필요합니다.");
        }
        if (request.getUploadType() != StudyMaterialUploadType.TEXT
                && (request.getFiles() == null || request.getFiles().isEmpty())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "파일 업로드는 최소 1개 파일이 필요합니다.");
        }
    }

    /**
     * Groq 텍스트 분류 프롬프트 생성
     */
    private String buildGroqPrompt(LectureMaterialAiProcessRequest request, String sourceText) {
        return """
                [입력]
                - 챕터명: %s
                - 파트 분류 방식: %s

                [분류 계획]
                %s

                [텍스트]
                %s

                [반환 규칙]
                - 반환 형식: {"parts":[{"partNumber":1,"name":"...","content":"..."}]}
                - partNumber는 1부터 시작하는 오름차순 정수
                - name은 1자 이상 30자 이하
                - content는 핵심 내용을 유지한 한국어 본문
                - partSplitMethod가 manual이면 분류 계획의 partNumber를 그대로 사용
                - partSplitMethod가 manual이고 intendedName이 있으면 반드시 그 이름 사용
                - parts는 최소 1개 이상 생성
                """.formatted(
                valueOrDefault(request.getChapterName(), "미지정"),
                request.getPartSplitMethod().getValue(),
                partSplitPlanContext(request.getPartSplitPlans()),
                valueOrDefault(sourceText, "")
        );
    }

    /**
     * Gemini OCR 및 분류 프롬프트 생성
     */
    private String buildGeminiPrompt(LectureMaterialAiProcessRequest request, String sourceText) {
        return """
                [작업]
                업로드 파일에서 텍스트를 추출(OCR 포함)한 뒤 파트를 분류한다.

                [입력]
                - 챕터명: %s
                - 파트 분류 방식: %s

                [분류 계획]
                %s

                [추가 텍스트]
                %s

                [반환 규칙]
                - 반환 형식: {"parts":[{"partNumber":1,"name":"...","content":"..."}]}
                - partNumber는 1부터 시작하는 오름차순 정수
                - name은 1자 이상 30자 이하
                - content는 핵심 내용을 유지한 한국어 본문
                - partSplitMethod가 manual이면 분류 계획의 partNumber를 그대로 사용
                - partSplitMethod가 manual이고 intendedName이 있으면 반드시 그 이름 사용
                - parts는 최소 1개 이상 생성
                """.formatted(
                valueOrDefault(request.getChapterName(), "미지정"),
                request.getPartSplitMethod().getValue(),
                partSplitPlanContext(request.getPartSplitPlans()),
                valueOrDefault(sourceText, "")
        );
    }

    /**
     * 수동 파트 분류 계획 프롬프트 컨텍스트 생성
     */
    private String partSplitPlanContext(List<LecturePartSplitPlan> plans) {
        if (plans == null || plans.isEmpty()) {
            return "- 계획 없음";
        }
        // 파트 번호 순서 기반 계획 정렬
        return plans.stream()
                .sorted(Comparator.comparing(LecturePartSplitPlan::getPartNumber))
                .map(plan -> "- partNumber: %d, intendedName: %s".formatted(
                        plan.getPartNumber(),
                        valueOrDefault(plan.getIntendedName(), "미지정")
                ))
                .collect(Collectors.joining("\n"));
    }

    /**
     * AI 파트 분류 응답 파싱
     */
    private List<LecturePartDraft> parseParts(String aiResponse) {
        try {
            String normalized = normalizeJson(aiResponse);
            LecturePartsPayload payload = objectMapper.readValue(normalized, LecturePartsPayload.class);
            if (payload.getParts() == null || payload.getParts().isEmpty()) {
                throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, "AI 응답에 파트 정보가 없습니다.");
            }

            List<LecturePartDraft> parts = new ArrayList<>();
            for (LecturePartPayload part : payload.getParts()) {
                // name/partName 호환 응답 처리
                String resolvedName = StringUtils.hasText(part.getName()) ? part.getName() : part.getPartName();
                if (part.getPartNumber() == null || !StringUtils.hasText(resolvedName)) {
                    throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, "AI 응답 파트 형식이 올바르지 않습니다.");
                }
                parts.add(LecturePartDraft.builder()
                        .partNumber(part.getPartNumber())
                        .name(resolvedName.trim())
                        .content(valueOrDefault(part.getContent(), "").trim())
                        .build());
            }
            return parts;
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, "AI 응답 파싱에 실패했습니다.", e);
        }
    }

    /**
     * AI JSON 응답 정규화
     */
    private String normalizeJson(String aiResponse) {
        if (!StringUtils.hasText(aiResponse)) {
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, "AI 응답이 비어 있습니다.");
        }
        String trimmed = aiResponse.trim();
        // 마크다운 코드블록 응답 제거
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n');
            int end = trimmed.lastIndexOf("```");
            if (start > -1 && end > start) {
                return trimmed.substring(start + 1, end).trim();
            }
        }
        return trimmed;
    }

    /**
     * 빈 문자열 기본값 처리
     */
    private String valueOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    @Getter
    @Setter
    private static class LecturePartsPayload {
        private List<LecturePartPayload> parts;
    }

    @Getter
    @Setter
    private static class LecturePartPayload {
        private Integer partNumber;
        private String name;
        private String partName;
        private String content;
    }
}
