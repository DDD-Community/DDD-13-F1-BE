package com.f1.quiket.domain.lecture.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.f1.quiket.domain.lecture.dto.LectureMaterialAiProcessRequest;
import com.f1.quiket.domain.lecture.dto.LectureMaterialAiProcessResult;
import com.f1.quiket.domain.lecture.dto.LectureMaterialFile;
import com.f1.quiket.domain.lecture.dto.LecturePartDraft;
import com.f1.quiket.domain.lecture.dto.LecturePartSplitPlan;
import com.f1.quiket.domain.lecture.dto.LecturePdfTextExtractionResult;
import com.f1.quiket.domain.lecture.dto.LectureUploadType;
import com.f1.quiket.domain.lecture.dto.PartSplitMethod;
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
 */
@Component
@RequiredArgsConstructor
public class LectureMaterialAiProcessor {

    private static final String SYSTEM_MESSAGE = """
            너는 Quiket 강의 자료 파트 분류 엔진이다.
            반드시 JSON만 반환한다.
            코드블록, 마크다운, 설명 문장은 절대 포함하지 않는다.
            """;

    private final LectureMaterialAiGateway lectureMaterialAiGateway;
    private final LecturePdfTextExtractor lecturePdfTextExtractor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LectureMaterialAiProcessResult process(LectureMaterialAiProcessRequest request) {
        validateRequest(request);

        return switch (request.getUploadType()) {
            case IMAGE -> processImage(request);
            case PDF -> processPdf(request);
            case TEXT -> processText(request);
        };
    }

    private LectureMaterialAiProcessResult processImage(LectureMaterialAiProcessRequest request) {
        String aiResponse = lectureMaterialAiGateway.analyzeImage(
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

    private LectureMaterialAiProcessResult processPdf(LectureMaterialAiProcessRequest request) {
        LectureMaterialFile pdfFile = request.getFiles().get(0);
        LecturePdfTextExtractionResult extractionResult = lecturePdfTextExtractor.extract(pdfFile);

        if (extractionResult.isHasTextLayer()) {
            String text = extractionResult.getExtractedText();
            String aiResponse = lectureMaterialAiGateway.analyzeText(
                    SYSTEM_MESSAGE,
                    buildGroqPrompt(request, text)
            );
            return LectureMaterialAiProcessResult.builder()
                    .provider("groq")
                    .extractedText(text)
                    .parts(parseParts(aiResponse))
                    .build();
        }

        String aiResponse = lectureMaterialAiGateway.analyzePdf(
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

    private LectureMaterialAiProcessResult processText(LectureMaterialAiProcessRequest request) {
        String aiResponse = lectureMaterialAiGateway.analyzeText(
                SYSTEM_MESSAGE,
                buildGroqPrompt(request, request.getText())
        );
        return LectureMaterialAiProcessResult.builder()
                .provider("groq")
                .extractedText(request.getText())
                .parts(parseParts(aiResponse))
                .build();
    }

    private void validateRequest(LectureMaterialAiProcessRequest request) {
        if (request == null || request.getUploadType() == null || request.getPartSplitMethod() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "강의 자료 AI 처리 요청값이 올바르지 않습니다.");
        }
        if (request.getUploadType() == LectureUploadType.TEXT && !StringUtils.hasText(request.getText())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "텍스트 업로드는 본문 텍스트가 필요합니다.");
        }
        if (request.getUploadType() != LectureUploadType.TEXT
                && (request.getFiles() == null || request.getFiles().isEmpty())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "파일 업로드는 최소 1개 파일이 필요합니다.");
        }
    }

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

    private String partSplitPlanContext(List<LecturePartSplitPlan> plans) {
        if (plans == null || plans.isEmpty()) {
            return "- 계획 없음";
        }
        return plans.stream()
                .sorted(Comparator.comparing(LecturePartSplitPlan::getPartNumber))
                .map(plan -> "- partNumber: %d, intendedName: %s".formatted(
                        plan.getPartNumber(),
                        valueOrDefault(plan.getIntendedName(), "미지정")
                ))
                .collect(Collectors.joining("\n"));
    }

    private List<LecturePartDraft> parseParts(String aiResponse) {
        try {
            String normalized = normalizeJson(aiResponse);
            LecturePartsPayload payload = objectMapper.readValue(normalized, LecturePartsPayload.class);
            if (payload.getParts() == null || payload.getParts().isEmpty()) {
                throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, "AI 응답에 파트 정보가 없습니다.");
            }

            List<LecturePartDraft> parts = new ArrayList<>();
            for (LecturePartPayload part : payload.getParts()) {
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

    private String normalizeJson(String aiResponse) {
        if (!StringUtils.hasText(aiResponse)) {
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, "AI 응답이 비어 있습니다.");
        }
        String trimmed = aiResponse.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n');
            int end = trimmed.lastIndexOf("```");
            if (start > -1 && end > start) {
                return trimmed.substring(start + 1, end).trim();
            }
        }
        return trimmed;
    }

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
