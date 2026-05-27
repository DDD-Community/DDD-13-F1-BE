package com.f1.quiket.domain.part.service;

import com.f1.quiket.domain.chapter.entity.Chapter;
import com.f1.quiket.domain.chapter.repository.ChapterRepository;
import com.f1.quiket.domain.lecture.entity.LectureUpload;
import com.f1.quiket.domain.lecture.repository.LectureUploadRepository;
import com.f1.quiket.domain.part.dto.PartResponse;
import com.f1.quiket.domain.part.dto.PartUpdateRequest;
import com.f1.quiket.domain.part.entity.Part;
import com.f1.quiket.domain.part.repository.PartRepository;
import com.f1.quiket.domain.subject.entity.Subject;
import com.f1.quiket.domain.subject.repository.SubjectRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 파트 조회 및 수정 서비스
 *
 * 공개 식별자 기반 파트 소유권 검증, 상세 응답 조립, 파트명/본문 변경을 담당
 */
@Service
@RequiredArgsConstructor
public class PartQueryService {

    private static final int MAX_NAME_LENGTH = 30;
    private static final int MAX_CONTENT_LENGTH = 30000;

    private final PartRepository partRepository;
    private final SubjectRepository subjectRepository;
    private final ChapterRepository chapterRepository;
    private final LectureUploadRepository lectureUploadRepository;

    /**
     * 파트 상세 조회
     *
     * @param userId 인증 사용자 내부 식별자
     * @param partPublicId 파트 공개 식별자
     * @return 파트 상세 응답
     */
    @Transactional(readOnly = true)
    public PartResponse getPart(Long userId, String partPublicId) {
        Part part = getOwnedPart(userId, partPublicId);
        return toResponse(userId, part);
    }

    /**
     * 파트명 및 본문 수정
     *
     * @param userId 인증 사용자 내부 식별자
     * @param partPublicId 파트 공개 식별자
     * @param request 수정 요청
     * @return 수정된 파트 상세 응답
     */
    @Transactional
    public PartResponse updatePart(Long userId, String partPublicId, PartUpdateRequest request) {
        Part part = getOwnedPart(userId, partPublicId);
        String name = normalizeName(request == null ? null : request.getName());
        String content = normalizeContent(request == null ? null : request.getContent());
        part.updateContent(name, content);
        return toResponse(userId, part);
    }

    private Part getOwnedPart(Long userId, String partPublicId) {
        if (!StringUtils.hasText(partPublicId)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "partId는 필수입니다.");
        }
        // 파트 소유권 검증
        return partRepository.findByPublicIdAndUserIdAndContentDeletedFalseAndDeletedAtIsNull(partPublicId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
    }

    private PartResponse toResponse(Long userId, Part part) {
        Subject subject = subjectRepository.findByIdAndUserIdAndDeletedAtIsNull(part.getSubjectId(), userId)
                .orElseThrow(() -> new CustomException(ErrorCode.SUBJECT_NOT_FOUND));
        Chapter chapter = chapterRepository.findById(part.getChapterId())
                .filter(found -> found.getDeletedAt() == null && found.getUserId().equals(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        LectureUpload lectureUpload = part.getLectureUploadId() == null
                ? null
                : lectureUploadRepository.findById(part.getLectureUploadId())
                        .filter(found -> found.getDeletedAt() == null && found.getUserId().equals(userId))
                        .orElse(null);
        return PartResponse.of(part, subject, chapter, lectureUpload);
    }

    private String normalizeName(String value) {
        if (!StringUtils.hasText(value)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "파트명을 입력해주세요");
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_NAME_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "파트명은 30자 이하로 입력해주세요");
        }
        return normalized;
    }

    private String normalizeContent(String value) {
        if (!StringUtils.hasText(value)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "파트 내용을 입력해주세요");
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_CONTENT_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "파트 내용은 30,000자 이하로 입력해주세요");
        }
        return normalized;
    }
}
