package com.f1.quiket.domain.quiz.dto;

import com.f1.quiket.domain.chapter.entity.Chapter;
import com.f1.quiket.domain.part.entity.Part;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 퀴즈 출제 범위 챕터 응답 DTO
 */
@Getter
@Builder
public class QuizScopeChapterResponse {

    private final String id;
    private final String subjectId;
    private final String name;
    private final Integer displayOrder;
    private final List<QuizScopePartResponse> parts;

    public static QuizScopeChapterResponse of(Chapter chapter, String subjectPublicId, List<Part> parts) {
        return QuizScopeChapterResponse.builder()
                .id(chapter.getPublicId())
                .subjectId(subjectPublicId)
                .name(chapter.getName())
                .displayOrder(chapter.getDisplayOrder())
                .parts(parts.stream()
                        .map(part -> QuizScopePartResponse.from(part, chapter.getPublicId()))
                        .toList())
                .build();
    }
}
