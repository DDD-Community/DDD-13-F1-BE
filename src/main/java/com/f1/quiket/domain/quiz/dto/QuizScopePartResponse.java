package com.f1.quiket.domain.quiz.dto;

import com.f1.quiket.domain.part.entity.Part;
import java.util.regex.Pattern;
import lombok.Builder;
import lombok.Getter;

/**
 * 퀴즈 출제 범위 파트 응답 DTO
 */
@Getter
@Builder
public class QuizScopePartResponse {

    private static final int CONTENT_PREVIEW_LENGTH = 30;
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private final String id;
    private final String chapterId;
    private final String name;
    private final Integer partNumber;
    private final String contentPreview;

    public static QuizScopePartResponse from(Part part, String chapterPublicId) {
        return QuizScopePartResponse.builder()
                .id(part.getPublicId())
                .chapterId(chapterPublicId)
                .name(part.getName())
                .partNumber(part.getPartNumber())
                .contentPreview(createContentPreview(part.getContent()))
                .build();
    }

    private static String createContentPreview(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }

        String normalizedContent = WHITESPACE_PATTERN.matcher(content).replaceAll(" ").trim();
        if (normalizedContent.length() <= CONTENT_PREVIEW_LENGTH) {
            return normalizedContent;
        }
        return normalizedContent.substring(0, CONTENT_PREVIEW_LENGTH) + "...";
    }
}
