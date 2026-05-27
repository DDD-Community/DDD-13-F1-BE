package com.f1.quiket.domain.part.dto;

import com.f1.quiket.domain.chapter.entity.Chapter;
import com.f1.quiket.domain.lecture.entity.LectureUpload;
import com.f1.quiket.domain.part.entity.Part;
import com.f1.quiket.domain.subject.entity.Subject;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.StringUtils;

/**
 * 파트 상세 응답 DTO
 *
 * 파트 공개 식별자와 소속 과목/챕터/업로드 공개 식별자, 본문을 전달
 */
@Getter
@Builder
public class PartResponse {

    private static final int PREVIEW_LENGTH = 30;

    private final String id;
    private final String subjectId;
    private final String chapterId;
    private final String lectureUploadId;
    private final String name;
    private final Integer partNumber;
    private final String content;
    private final String contentPreview;

    /**
     * 엔티티 기반 파트 상세 응답 생성
     */
    public static PartResponse of(
            Part part,
            Subject subject,
            Chapter chapter,
            LectureUpload lectureUpload
    ) {
        return PartResponse.builder()
                .id(part.getPublicId())
                .subjectId(subject.getPublicId())
                .chapterId(chapter.getPublicId())
                .lectureUploadId(lectureUpload == null ? null : lectureUpload.getPublicId())
                .name(part.getName())
                .partNumber(part.getPartNumber())
                .content(part.getContent())
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
