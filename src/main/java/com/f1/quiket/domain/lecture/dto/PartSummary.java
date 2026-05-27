package com.f1.quiket.domain.lecture.dto;

import com.f1.quiket.domain.chapter.entity.Chapter;
import com.f1.quiket.domain.part.entity.Part;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.StringUtils;

/**
 * 파트 요약 응답 DTO
 *
 * 상태 조회와 챕터 목록에서 사용할 파트 기본 정보 전달
 */
@Getter
@Builder
public class PartSummary {
    private static final int PREVIEW_LENGTH = 30;

    private final String id;
    private final String chapterId;
    private final String name;
    private final Integer partNumber;
    private final String contentPreview;

    /**
     * 파트 요약 응답 생성
     */
    public static PartSummary of(Part part, Chapter chapter) {
        return PartSummary.builder()
                .id(part.getPublicId())
                .chapterId(chapter.getPublicId())
                .name(part.getName())
                .partNumber(part.getPartNumber())
                .contentPreview(preview(part.getContent()))
                .build();
    }

    private static String preview(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        String normalized = content.trim();
        if (normalized.length() <= PREVIEW_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, PREVIEW_LENGTH);
    }
}
