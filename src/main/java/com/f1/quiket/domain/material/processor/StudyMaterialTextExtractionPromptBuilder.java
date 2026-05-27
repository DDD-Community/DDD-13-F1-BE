package com.f1.quiket.domain.material.processor;

import com.f1.quiket.domain.material.dto.StudyMaterialAiPrompt;
import org.springframework.stereotype.Component;

/**
 * 학습 자료 텍스트 추출 프롬프트 생성기
 *
 * OCR 전용 시스템 메시지와 사용자 메시지 생성
 */
@Component
public class StudyMaterialTextExtractionPromptBuilder {

    /**
     * OCR 텍스트 추출 프롬프트 생성
     */
    public StudyMaterialAiPrompt buildOcrPrompt() {
        return new StudyMaterialAiPrompt(systemMessage(), userMessage());
    }

    private String systemMessage() {
        return """
                너는 Quiket 학습 자료 OCR 엔진이다.
                반드시 추출된 텍스트만 반환한다.
                설명, 마크다운, 코드블록은 절대 포함하지 않는다.
                """;
    }

    private String userMessage() {
        return """
                업로드 파일의 모든 학습 텍스트를 읽기 순서대로 추출한다.
                표와 목록은 의미가 유지되도록 줄바꿈으로 정리한다.
                파트 분류, 제목 생성, 요약은 수행하지 않는다.
                """;
    }
}
