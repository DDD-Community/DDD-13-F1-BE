package com.f1.quiket.domain.lecture.service;

import com.f1.quiket.domain.lecture.dto.LectureMaterialAiProcessRequest;
import com.f1.quiket.domain.lecture.dto.LecturePartSplitPlan;
import com.f1.quiket.domain.material.dto.StudyMaterialAiPrompt;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 강의 자료 AI 프롬프트 생성기
 *
 * OCR 및 파트 분류용 시스템 메시지와 사용자 메시지 생성
 */
@Component
public class LectureMaterialAiPromptBuilder {

    /**
     * Groq 텍스트 분류 프롬프트 생성
     */
    public StudyMaterialAiPrompt buildGroqPrompt(LectureMaterialAiProcessRequest request, String sourceText) {
        return new StudyMaterialAiPrompt(systemMessage(), groqUserMessage(request, sourceText));
    }

    /**
     * Gemini OCR 및 분류 프롬프트 생성
     */
    public StudyMaterialAiPrompt buildGeminiPrompt(LectureMaterialAiProcessRequest request, String sourceText) {
        return new StudyMaterialAiPrompt(systemMessage(), geminiUserMessage(request, sourceText));
    }

    private String systemMessage() {
        return """
                너는 Quiket 강의 자료 파트 분류 엔진이다.
                역할은 입력 자료를 파트 단위로 나누는 것이며 요약 엔진이 아니다.
                반드시 JSON만 반환한다.
                코드블록, 마크다운, 설명 문장은 절대 포함하지 않는다.
                """;
    }

    private String groqUserMessage(LectureMaterialAiProcessRequest request, String sourceText) {
        return """
                [입력]
                - 챕터명: %s
                - 파트 분류 방식: %s

                [분류 계획]
                %s

                [줄 번호가 붙은 텍스트]
                %s

                [반환 규칙]
                - 반환 형식: {"parts":[{"partNumber":1,"name":"...","startLine":1,"endLine":10}]}
                - partNumber는 1부터 시작하는 오름차순 정수
                - name은 1자 이상 30자 이하
                - startLine과 endLine은 해당 파트에 속한 원문 줄 번호 범위
                - 줄 번호 범위는 1부터 마지막 줄까지 누락 없이 연속되어야 함
                - 줄 번호 범위는 서로 겹치면 안 됨
                - 첫 번째 part의 startLine은 반드시 1
                - 이전 part의 endLine + 1은 반드시 다음 part의 startLine
                - 예: part1.endLine이 20이면 part2.startLine은 반드시 21
                - 마지막 part의 endLine은 반드시 입력 텍스트의 마지막 줄 번호
                - content 필드는 절대 반환하지 않음
                - partSplitMethod가 manual이면 분류 계획의 partNumber를 그대로 사용
                - partSplitMethod가 manual이고 intendedName이 있으면 반드시 그 이름 사용
                - parts는 최소 1개 이상 생성
                """.formatted(
                valueOrDefault(request.getChapterName(), "미지정"),
                request.getPartSplitMethod().getValue(),
                partSplitPlanContext(request.getPartSplitPlans()),
                lineNumberedText(sourceText)
        );
    }

    private String geminiUserMessage(LectureMaterialAiProcessRequest request, String sourceText) {
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
                - content는 해당 파트에 속한 OCR 추출 원문 전체
                - content는 요약, 압축, 재작성, 해설 추가 금지
                - content는 문장, 목록, 표의 의미 단위를 누락 없이 유지
                - content는 OCR 오류로 인한 명백한 줄바꿈만 자연스럽게 정리
                - 업로드 파일에서 읽은 모든 내용은 parts 중 하나의 content에 반드시 포함
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
        // 파트 번호 순서 기반 계획 정렬
        return plans.stream()
                .sorted(Comparator.comparing(LecturePartSplitPlan::getPartNumber))
                .map(plan -> "- partNumber: %d, intendedName: %s".formatted(
                        plan.getPartNumber(),
                        valueOrDefault(plan.getIntendedName(), "미지정")
                ))
                .collect(Collectors.joining("\n"));
    }

    private String lineNumberedText(String sourceText) {
        String text = valueOrDefault(sourceText, "");
        String[] lines = text.split("\\R", -1);
        if (lines.length > 1 && lines[lines.length - 1].isEmpty()) {
            lines = java.util.Arrays.copyOf(lines, lines.length - 1);
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            builder.append(i + 1)
                    .append("| ")
                    .append(lines[i]);
            if (i < lines.length - 1) {
                builder.append('\n');
            }
        }
        return builder.toString();
    }

    private String valueOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
