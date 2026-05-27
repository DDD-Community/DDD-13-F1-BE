package com.f1.quiket.domain.lecture.dto;

import com.f1.quiket.domain.chapter.entity.Chapter;
import com.f1.quiket.domain.lecture.entity.LectureUpload;
import com.f1.quiket.domain.subject.entity.Subject;
import lombok.Builder;
import lombok.Getter;

/**
 * 강의 업로드 접수 응답 DTO
 *
 * 외부 공개 식별자와 최초 처리 상태 전달
 */
@Getter
@Builder
public class LectureUploadAcceptedResponse {
    private final String lectureUploadId;
    private final String subjectId;
    private final String chapterId;
    private final String status;
    private final Integer estimatedSeconds;

    /**
     * 업로드 접수 응답 생성
     */
    public static LectureUploadAcceptedResponse of(LectureUpload upload, Subject subject, Chapter chapter) {
        return LectureUploadAcceptedResponse.builder()
                .lectureUploadId(upload.getPublicId())
                .subjectId(subject.getPublicId())
                .chapterId(chapter.getPublicId())
                .status(upload.getStatus())
                .estimatedSeconds(30)
                .build();
    }
}
