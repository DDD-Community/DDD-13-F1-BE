package com.f1.quiket.domain.lecture.dto;

import com.f1.quiket.domain.chapter.entity.Chapter;
import com.f1.quiket.domain.lecture.entity.LectureUpload;
import com.f1.quiket.domain.lecture.entity.LectureUploadStatus;
import com.f1.quiket.domain.part.entity.Part;
import com.f1.quiket.domain.subject.entity.Subject;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 강의 업로드 상태 조회 응답 DTO
 *
 * 처리 상태, 진행률, 생성 파트 목록, 실패 사유 전달
 */
@Getter
@Builder
public class LectureUploadStatusResponse {
    private final String lectureUploadId;
    private final String subjectId;
    private final String chapterId;
    private final String status;
    private final Integer estimatedSeconds;
    private final Integer progressPct;
    private final List<PartSummary> parts;
    private final String failReason;

    /**
     * 업로드 상태 응답 생성
     */
    public static LectureUploadStatusResponse of(
            LectureUpload upload,
            Subject subject,
            Chapter chapter,
            List<Part> parts
    ) {
        return LectureUploadStatusResponse.builder()
                .lectureUploadId(upload.getPublicId())
                .subjectId(subject.getPublicId())
                .chapterId(chapter.getPublicId())
                .status(upload.getStatus())
                .estimatedSeconds(30)
                .progressPct(progressPct(upload))
                .parts(parts.stream()
                        .map(part -> PartSummary.of(part, chapter))
                        .toList())
                .failReason(upload.getFailReason())
                .build();
    }

    private static int progressPct(LectureUpload upload) {
        LectureUploadStatus status = LectureUploadStatus.from(upload.getStatus());
        return switch (status) {
            case PENDING -> 0;
            case PROCESSING -> 50;
            case COMPLETED, FAILED -> 100;
        };
    }
}
