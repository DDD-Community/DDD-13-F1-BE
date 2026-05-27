package com.f1.quiket.domain.lecture.service;

import com.f1.quiket.domain.chapter.entity.Chapter;
import com.f1.quiket.domain.chapter.repository.ChapterRepository;
import com.f1.quiket.domain.lecture.dto.LectureUploadStatusResponse;
import com.f1.quiket.domain.lecture.entity.LectureProcessingJob;
import com.f1.quiket.domain.lecture.entity.LectureUpload;
import com.f1.quiket.domain.lecture.repository.LectureProcessingJobRepository;
import com.f1.quiket.domain.lecture.repository.LectureUploadRepository;
import com.f1.quiket.domain.part.entity.Part;
import com.f1.quiket.domain.part.repository.PartRepository;
import com.f1.quiket.domain.subject.entity.Subject;
import com.f1.quiket.domain.subject.repository.SubjectRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 강의 업로드 처리 상태 조회 서비스
 *
 * 본인 소유 업로드의 상태, 진행률, 생성 파트 목록, 실패 사유를 조회
 */
@Service
@RequiredArgsConstructor
public class LectureUploadStatusService {

    private final LectureUploadRepository lectureUploadRepository;
    private final LectureProcessingJobRepository lectureProcessingJobRepository;
    private final ChapterRepository chapterRepository;
    private final SubjectRepository subjectRepository;
    private final PartRepository partRepository;

    /**
     * 강의 업로드 처리 상태 조회
     *
     * @param userId 인증 사용자 내부 식별자
     * @param lectureUploadPublicId 업로드 공개 식별자
     * @return 업로드 상태 응답
     */
    @Transactional(readOnly = true)
    public LectureUploadStatusResponse getStatus(Long userId, String lectureUploadPublicId) {
        // 업로드 소유권 검증
        LectureUpload upload = lectureUploadRepository
                .findByPublicIdAndUserIdAndDeletedAtIsNull(lectureUploadPublicId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        Chapter chapter = chapterRepository.findById(upload.getChapterId())
                .filter(found -> found.getDeletedAt() == null)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        Subject subject = subjectRepository.findByIdAndUserIdAndDeletedAtIsNull(chapter.getSubjectId(), userId)
                .orElseThrow(() -> new CustomException(ErrorCode.SUBJECT_NOT_FOUND));
        List<Part> parts = partRepository.findAllByLectureUploadIdAndUserIdAndDeletedAtIsNullOrderByPartNumberAsc(
                upload.getId(),
                userId
        );
        LectureProcessingJob processingJob = lectureProcessingJobRepository.findByLectureUploadId(upload.getId())
                .orElse(null);

        return LectureUploadStatusResponse.of(upload, processingJob, subject, chapter, parts);
    }
}
