package com.f1.quiket.domain.lecture.dto;

import com.f1.quiket.domain.chapter.entity.Chapter;
import com.f1.quiket.domain.lecture.entity.LectureProcessingJob;
import com.f1.quiket.global.response.ErrorCode;
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
 * 처리 상태, 진행률, 챕터명, 생성 파트 목록, 실패 사유 전달
 * chapterName은 AI 처리 완료 후 갱신된 최종값을 반환 (처리 중에는 임시명 "새 챕터")
 */
@Getter
@Builder
public class LectureUploadStatusResponse {
    private final String lectureUploadId;
    private final String subjectId;
    private final String chapterId;
    /**
     * 챕터명
     * AI 처리 완료 전: 임시명("새 챕터"), 완료 후: AI 생성 또는 사용자 입력 챕터명
     */
    private final String chapterName;
    private final String status;
    private final Integer estimatedSeconds;
    private final Integer progressPct;
    private final List<PartSummary> parts;
    private final String failCode;
    private final String failMessage;

    /**
     * 업로드 상태 응답 생성
     */
    public static LectureUploadStatusResponse of(
            LectureUpload upload,
            LectureProcessingJob processingJob,
            Subject subject,
            Chapter chapter,
            List<Part> parts
    ) {
        return LectureUploadStatusResponse.builder()
                .lectureUploadId(upload.getPublicId())
                .subjectId(subject.getPublicId())
                .chapterId(chapter.getPublicId())
                .chapterName(chapter.getName())
                .status(upload.getStatus())
                .estimatedSeconds(30)
                .progressPct(progressPct(upload))
                .parts(parts.stream()
                        .map(part -> PartSummary.of(part, chapter))
                        .toList())
                .failCode(processingJob == null ? null : processingJob.getFailCode())
                .failMessage(resolveFailMessage(processingJob))
                .build();
    }

    private static String resolveFailMessage(LectureProcessingJob processingJob) {
        if (processingJob == null || processingJob.getFailCode() == null) {
            return null;
        }
        try {
            return ErrorCode.valueOf(processingJob.getFailCode()).getMessage();
        } catch (IllegalArgumentException e) {
            return null;
        }
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
