package com.f1.quiket.domain.quiz.dto;

import com.f1.quiket.domain.chapter.entity.Chapter;
import com.f1.quiket.domain.part.entity.Part;
import com.f1.quiket.domain.subject.entity.Subject;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/**
 * 퀴즈 출제 범위 응답 DTO
 */
@Getter
@Builder
public class QuizScopeResponse {

    private final String subjectId;
    private final String subjectName;
    private final List<QuizScopeChapterResponse> chapters;

    public static QuizScopeResponse of(
            Subject subject,
            List<Chapter> chapters,
            Map<Long, List<Part>> partsByChapterId
    ) {
        return QuizScopeResponse.builder()
                .subjectId(subject.getPublicId())
                .subjectName(subject.getName())
                .chapters(chapters.stream()
                        .map(chapter -> QuizScopeChapterResponse.of(
                                chapter,
                                subject.getPublicId(),
                                partsByChapterId.getOrDefault(chapter.getId(), List.of())
                        ))
                        .toList())
                .build();
    }
}
