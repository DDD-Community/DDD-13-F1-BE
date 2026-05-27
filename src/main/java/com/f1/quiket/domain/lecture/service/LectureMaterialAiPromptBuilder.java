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
        // 파트 번호 순서 기반 계획 정렬
        return plans.stream()
                .sorted(Comparator.comparing(LecturePartSplitPlan::getPartNumber))
                .map(plan -> "- partNumber: %d, intendedName: %s".formatted(
                        plan.getPartNumber(),
                        valueOrDefault(plan.getIntendedName(), "미지정")
                ))
                .collect(Collectors.joining("\n"));
    }

    private String valueOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
