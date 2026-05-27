package com.f1.quiket.domain.lecture.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.f1.quiket.domain.lecture.dto.LectureMaterialAiProcessRequest;
import com.f1.quiket.domain.lecture.dto.LectureMaterialAiProcessResult;
import com.f1.quiket.domain.lecture.dto.LecturePartDraft;
import com.f1.quiket.domain.material.dto.StudyMaterialAiPrompt;
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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 강의 자료 AI 처리 오케스트레이터
 *
 * 강의 업로드 자료의 OCR, 텍스트 추출, 파트 분류 흐름 조합
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LectureMaterialAiProcessor {

    private static final String UNCLASSIFIED_PART_NAME = "미분류";

    private final StudyMaterialAiGateway studyMaterialAiGateway;
    private final StudyMaterialPdfTextExtractor studyMaterialPdfTextExtractor;
    private final LectureMaterialAiPromptBuilder promptBuilder;
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
        StudyMaterialAiPrompt prompt = promptBuilder.buildGeminiPrompt(request, null);
        // Gemini 기반 이미지 OCR 및 파트 분류
        String aiResponse = studyMaterialAiGateway.generateFromImages(
                prompt.systemMessage(),
                prompt.userMessage(),
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
            StudyMaterialAiPrompt prompt = promptBuilder.buildGroqPrompt(request, text);
            String aiResponse = studyMaterialAiGateway.generateFromText(
                    prompt.systemMessage(),
                    prompt.userMessage()
            );
            return LectureMaterialAiProcessResult.builder()
                    .provider("groq")
                    .extractedText(text)
                    .parts(parseGroqParts(aiResponse, text))
                    .build();
        }

        StudyMaterialAiPrompt prompt = promptBuilder.buildGeminiPrompt(request, null);
        // 스캔 PDF는 Gemini OCR 및 파트 분류
        String aiResponse = studyMaterialAiGateway.generateFromPdf(
                prompt.systemMessage(),
                prompt.userMessage(),
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
        StudyMaterialAiPrompt prompt = promptBuilder.buildGroqPrompt(request, request.getText());
        // 입력 텍스트 기반 Groq 파트 분류
        String aiResponse = studyMaterialAiGateway.generateFromText(
                prompt.systemMessage(),
                prompt.userMessage()
        );
        return LectureMaterialAiProcessResult.builder()
                .provider("groq")
                .extractedText(request.getText())
                .parts(parseGroqParts(aiResponse, request.getText()))
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
     * Groq 라인 범위 응답 파싱 및 원문 분리
     */
    private List<LecturePartDraft> parseGroqParts(String aiResponse, String sourceText) {
        try {
            String normalized = normalizeJson(aiResponse);
            LecturePartsPayload payload = objectMapper.readValue(normalized, LecturePartsPayload.class);
            List<LineSpan> lineSpans = lineSpans(sourceText);
            if (payload.getParts() == null || payload.getParts().isEmpty()) {
                log.warn("Groq 파트 범위 응답 없음, 전체 원문 미분류 처리 totalLineCount={}", lineSpans.size());
                return List.of(unclassifiedPart(1, sourceText, lineSpans, 1, lineSpans.size()));
            }

            List<LecturePartPayload> sortedParts = payload.getParts().stream()
                    .filter(this::isValidGroqPartPayload)
                    .sorted(Comparator.comparing(LecturePartPayload::getPartNumber))
                    .toList();
            if (sortedParts.size() != payload.getParts().size()) {
                log.warn(
                        "Groq 파트 범위 응답 형식 일부 무시 requestedParts={}, validParts={}",
                        payload.getParts().size(),
                        sortedParts.size()
                );
            }
            if (sortedParts.isEmpty()) {
                log.warn("Groq 유효 파트 범위 없음, 전체 원문 미분류 처리 totalLineCount={}", lineSpans.size());
                return List.of(unclassifiedPart(1, sourceText, lineSpans, 1, lineSpans.size()));
            }

            List<LecturePartDraft> parts = new ArrayList<>();
            int nextSourceLine = 1;
            int nextPartNumber = 1;
            for (LecturePartPayload part : sortedParts) {
                int startLine = Math.max(1, part.getStartLine());
                int endLine = Math.min(lineSpans.size(), part.getEndLine());
                if (startLine > lineSpans.size() || endLine < 1) {
                    log.warn(
                            "Groq 파트 범위 원문 밖 위치 무시 partNumber={}, startLine={}, endLine={}, totalLineCount={}",
                            part.getPartNumber(),
                            part.getStartLine(),
                            part.getEndLine(),
                            lineSpans.size()
                    );
                    continue;
                }
                if (startLine > nextSourceLine) {
                    log.warn(
                            "Groq 파트 누락 범위 미분류 처리 missingStartLine={}, missingEndLine={}",
                            nextSourceLine,
                            startLine - 1
                    );
                    nextPartNumber = addUnclassifiedPart(
                            parts,
                            nextPartNumber,
                            sourceText,
                            lineSpans,
                            nextSourceLine,
                            startLine - 1
                    );
                }
                if (startLine < nextSourceLine) {
                    log.warn(
                            "Groq 파트 중복 범위 보정 partNumber={}, originalStartLine={}, adjustedStartLine={}",
                            part.getPartNumber(),
                            startLine,
                            nextSourceLine
                    );
                    startLine = nextSourceLine;
                }
                if (endLine < startLine) {
                    log.warn(
                            "Groq 파트 범위 중복으로 무시 partNumber={}, startLine={}, endLine={}",
                            part.getPartNumber(),
                            startLine,
                            endLine
                    );
                    continue;
                }

                LineSpan start = lineSpans.get(startLine - 1);
                LineSpan end = lineSpans.get(endLine - 1);
                String content = sourceText.substring(start.startIndex(), end.endIndex());
                nextPartNumber = addPart(parts, nextPartNumber, resolvePartName(part), content);
                nextSourceLine = endLine + 1;
            }

            if (nextSourceLine <= lineSpans.size()) {
                log.warn(
                        "Groq 마지막 누락 범위 미분류 처리 missingStartLine={}, missingEndLine={}",
                        nextSourceLine,
                        lineSpans.size()
                );
                addUnclassifiedPart(parts, nextPartNumber, sourceText, lineSpans, nextSourceLine, lineSpans.size());
            }
            return parts;
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, "AI 응답 파싱에 실패했습니다.", e);
        }
    }

    private boolean isValidGroqPartPayload(LecturePartPayload part) {
        return part != null
                && part.getPartNumber() != null
                && StringUtils.hasText(resolvePartName(part))
                && part.getStartLine() != null
                && part.getEndLine() != null
                && part.getEndLine() >= part.getStartLine();
    }

    private String resolvePartName(LecturePartPayload part) {
        String resolvedName = StringUtils.hasText(part.getName()) ? part.getName() : part.getPartName();
        return resolvedName == null ? "" : resolvedName.trim();
    }

    private LecturePartDraft unclassifiedPart(
            int partNumber,
            String sourceText,
            List<LineSpan> lineSpans,
            int startLine,
            int endLine
    ) {
        LineSpan start = lineSpans.get(startLine - 1);
        LineSpan end = lineSpans.get(endLine - 1);
        return LecturePartDraft.builder()
                .partNumber(partNumber)
                .name(UNCLASSIFIED_PART_NAME)
                .content(sourceText.substring(start.startIndex(), end.endIndex()))
                .build();
    }

    private int addUnclassifiedPart(
            List<LecturePartDraft> parts,
            int partNumber,
            String sourceText,
            List<LineSpan> lineSpans,
            int startLine,
            int endLine
    ) {
        LecturePartDraft part = unclassifiedPart(partNumber, sourceText, lineSpans, startLine, endLine);
        return addPart(parts, partNumber, part.getName(), part.getContent());
    }

    private int addPart(List<LecturePartDraft> parts, int partNumber, String name, String content) {
        if (!StringUtils.hasText(content)) {
            log.warn("Groq 공백 파트 범위 무시 partNumber={}, name={}", partNumber, name);
            return partNumber;
        }
        parts.add(LecturePartDraft.builder()
                .partNumber(partNumber)
                .name(name)
                .content(content)
                .build());
        return partNumber + 1;
    }

    /**
     * 원문 라인별 문자 범위 계산
     */
    private List<LineSpan> lineSpans(String sourceText) {
        if (!StringUtils.hasText(sourceText)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "분리할 원문 텍스트가 비어 있습니다.");
        }

        List<LineSpan> spans = new ArrayList<>();
        int start = 0;
        while (start < sourceText.length()) {
            int end = nextLineEnd(sourceText, start);
            spans.add(new LineSpan(start, end));
            start = end;
        }
        return spans;
    }

    private int nextLineEnd(String sourceText, int start) {
        for (int i = start; i < sourceText.length(); i++) {
            char current = sourceText.charAt(i);
            if (current == '\n') {
                return i + 1;
            }
            if (current == '\r') {
                return i + 1 < sourceText.length() && sourceText.charAt(i + 1) == '\n' ? i + 2 : i + 1;
            }
        }
        return sourceText.length();
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
        private Integer startLine;
        private Integer endLine;
    }

    private record LineSpan(int startIndex, int endIndex) {
    }
}
