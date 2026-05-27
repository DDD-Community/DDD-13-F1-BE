package com.f1.quiket.domain.part.service;

import com.f1.quiket.domain.chapter.entity.Chapter;
import com.f1.quiket.domain.chapter.repository.ChapterRepository;
import com.f1.quiket.domain.lecture.dto.LectureUploadAcceptedResponse;
import com.f1.quiket.domain.lecture.dto.PartSplitMethod;
import com.f1.quiket.domain.lecture.entity.LectureProcessingJob;
import com.f1.quiket.domain.lecture.entity.LectureUpload;
import com.f1.quiket.domain.lecture.entity.LectureUploadFile;
import com.f1.quiket.domain.lecture.repository.LectureProcessingJobRepository;
import com.f1.quiket.domain.lecture.repository.LectureUploadFileRepository;
import com.f1.quiket.domain.lecture.repository.LectureUploadRepository;
import com.f1.quiket.domain.material.dto.StudyMaterialFile;
import com.f1.quiket.domain.material.dto.StudyMaterialUploadType;
import com.f1.quiket.domain.part.dto.PartTextAddRequest;
import com.f1.quiket.domain.part.event.PartAddProcessingRequestedEvent;
import com.f1.quiket.domain.part.repository.PartRepository;
import com.f1.quiket.domain.subject.entity.Subject;
import com.f1.quiket.domain.subject.repository.SubjectRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 기존 챕터 파트 추가 접수 서비스
 *
 * 요청 검증, 챕터/업로드/파일 메타데이터 저장, 단일 파트 추가 처리 이벤트 발행을 담당
 */
@Service
@RequiredArgsConstructor
public class PartAddCreateService {

    private static final long MAX_UPLOAD_SIZE_BYTES = 50L * 1024L * 1024L;
    private static final int MIN_TEXT_LENGTH = 100;
    private static final int MAX_TEXT_LENGTH = 30000;
    private static final int MAX_PART_NAME_LENGTH = 30;
    private static final int ESTIMATED_SECONDS = 30;

    private final ChapterRepository chapterRepository;
    private final SubjectRepository subjectRepository;
    private final PartRepository partRepository;
    private final LectureUploadRepository lectureUploadRepository;
    private final LectureUploadFileRepository lectureUploadFileRepository;
    private final LectureProcessingJobRepository lectureProcessingJobRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 텍스트 직접 입력 파트 추가 접수
     *
     * @param userId 인증 사용자 내부 식별자
     * @param chapterPublicId 챕터 공개 식별자
     * @param request 텍스트 파트 추가 요청
     * @return 업로드 접수 응답
     */
    @Transactional
    public LectureUploadAcceptedResponse createTextPart(
            Long userId,
            String chapterPublicId,
            PartTextAddRequest request
    ) {
        String partName = normalizePartName(request == null ? null : request.getPartName());
        StudyMaterialUploadType uploadType = parseUploadType(request == null ? null : request.getUploadType());
        validateTextUpload(uploadType);
        String text = normalizeText(request == null ? null : request.getText());

        Chapter chapter = getOwnedChapter(userId, chapterPublicId);
        Subject subject = getOwnedSubject(userId, chapter.getSubjectId());
        int nextPartNumber = partRepository.findMaxPartNumberByChapterId(chapter.getId()) + 1;
        LectureUpload upload = saveUpload(chapter, userId, uploadType);
        upload.storeRawText(text);
        saveProcessingJob(upload.getId(), userId);

        eventPublisher.publishEvent(new PartAddProcessingRequestedEvent(
                upload.getId(),
                chapter.getId(),
                subject.getId(),
                userId,
                partName,
                nextPartNumber,
                uploadType,
                text,
                List.of()
        ));

        return LectureUploadAcceptedResponse.of(upload, subject, chapter);
    }

    /**
     * PDF 또는 이미지 파일 기반 파트 추가 접수
     *
     * @param userId 인증 사용자 내부 식별자
     * @param chapterPublicId 챕터 공개 식별자
     * @param partNameValue 파트명
     * @param uploadTypeValue pdf 또는 image
     * @param multipartFiles 업로드 파일 목록
     * @return 업로드 접수 응답
     */
    @Transactional
    public LectureUploadAcceptedResponse createFilePart(
            Long userId,
            String chapterPublicId,
            String partNameValue,
            String uploadTypeValue,
            List<MultipartFile> multipartFiles
    ) {
        String partName = normalizePartName(partNameValue);
        StudyMaterialUploadType uploadType = parseUploadType(uploadTypeValue);
        List<StudyMaterialFile> files = validateAndReadFiles(uploadType, multipartFiles);

        Chapter chapter = getOwnedChapter(userId, chapterPublicId);
        Subject subject = getOwnedSubject(userId, chapter.getSubjectId());
        int nextPartNumber = partRepository.findMaxPartNumberByChapterId(chapter.getId()) + 1;
        LectureUpload upload = saveUpload(chapter, userId, uploadType);
        saveProcessingJob(upload.getId(), userId);
        saveUploadFiles(upload, uploadType, files);

        eventPublisher.publishEvent(new PartAddProcessingRequestedEvent(
                upload.getId(),
                chapter.getId(),
                subject.getId(),
                userId,
                partName,
                nextPartNumber,
                uploadType,
                null,
                files
        ));

        return LectureUploadAcceptedResponse.of(upload, subject, chapter);
    }

    private Chapter getOwnedChapter(Long userId, String chapterPublicId) {
        if (!StringUtils.hasText(chapterPublicId)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "chapterId는 필수입니다.");
        }
        // 챕터 소유권 검증
        return chapterRepository.findForUpdateByPublicIdAndUserIdAndDeletedAtIsNull(chapterPublicId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
    }

    private Subject getOwnedSubject(Long userId, Long subjectId) {
        return subjectRepository.findByIdAndUserIdAndDeletedAtIsNull(subjectId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.SUBJECT_NOT_FOUND));
    }

    private LectureUpload saveUpload(Chapter chapter, Long userId, StudyMaterialUploadType uploadType) {
        return lectureUploadRepository.save(
                LectureUpload.create(chapter.getId(), userId, uploadType, PartSplitMethod.MANUAL)
        );
    }

    private StudyMaterialUploadType parseUploadType(String value) {
        try {
            return StudyMaterialUploadType.from(value);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "지원하지 않는 업로드 타입입니다.");
        }
    }

    private String normalizePartName(String value) {
        if (!StringUtils.hasText(value)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "파트명을 입력해주세요");
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_PART_NAME_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "파트명은 30자 이하로 입력해주세요");
        }
        return normalized;
    }

    private String normalizeText(String text) {
        if (!StringUtils.hasText(text)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "파트 내용을 입력해주세요");
        }
        String normalized = text.trim();
        if (normalized.length() < MIN_TEXT_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "text는 100자 이상 입력해주세요.");
        }
        if (normalized.length() > MAX_TEXT_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "파트 내용은 30,000자 이하로 입력해주세요");
        }
        return normalized;
    }

    private void validateTextUpload(StudyMaterialUploadType uploadType) {
        if (uploadType != StudyMaterialUploadType.TEXT) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "텍스트 업로드는 uploadType=text만 사용할 수 있습니다.");
        }
    }

    private List<StudyMaterialFile> validateAndReadFiles(
            StudyMaterialUploadType uploadType,
            List<MultipartFile> multipartFiles
    ) {
        if (uploadType == StudyMaterialUploadType.TEXT) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "파일 업로드는 uploadType=pdf 또는 image만 사용할 수 있습니다.");
        }
        if (multipartFiles == null || multipartFiles.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "files는 필수입니다.");
        }
        if (uploadType == StudyMaterialUploadType.PDF && multipartFiles.size() != 1) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "PDF 파일은 1개만 업로드할 수 있습니다.");
        }

        long totalSize = multipartFiles.stream().mapToLong(MultipartFile::getSize).sum();
        if (totalSize > MAX_UPLOAD_SIZE_BYTES) {
            throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED, "파일 크기는 50MB 이하만 업로드 가능합니다");
        }

        List<StudyMaterialFile> files = new ArrayList<>();
        for (MultipartFile multipartFile : multipartFiles) {
            validateFileType(uploadType, multipartFile);
            try {
                files.add(StudyMaterialFile.builder()
                        .fileName(resolveFileName(multipartFile))
                        .contentType(resolveContentType(multipartFile))
                        .bytes(multipartFile.getBytes())
                        .build());
            } catch (IOException e) {
                throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, "업로드에 실패했어요. 다시 시도해주세요", e);
            }
        }
        return files;
    }

    private void validateFileType(StudyMaterialUploadType uploadType, MultipartFile file) {
        String filename = resolveFileName(file).toLowerCase(Locale.ROOT);
        String contentType = resolveContentType(file).toLowerCase(Locale.ROOT);
        // 파일 형식 검증
        if (uploadType == StudyMaterialUploadType.PDF
                && !("application/pdf".equals(contentType) || filename.endsWith(".pdf"))) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "지원하지 않는 파일 형식이에요");
        }
        if (uploadType == StudyMaterialUploadType.IMAGE
                && !(isSupportedImageContentType(contentType) || isSupportedImageFileName(filename))) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "JPG, PNG 형식만 지원해요");
        }
    }

    private boolean isSupportedImageContentType(String contentType) {
        return "image/jpeg".equals(contentType) || "image/png".equals(contentType);
    }

    private boolean isSupportedImageFileName(String filename) {
        return filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png");
    }

    private String resolveFileName(MultipartFile file) {
        return StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "upload";
    }

    private String resolveContentType(MultipartFile file) {
        return StringUtils.hasText(file.getContentType()) ? file.getContentType() : "application/octet-stream";
    }

    private void saveUploadFiles(
            LectureUpload upload,
            StudyMaterialUploadType uploadType,
            List<StudyMaterialFile> files
    ) {
        List<LectureUploadFile> uploadFiles = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            StudyMaterialFile file = files.get(i);
            uploadFiles.add(LectureUploadFile.create(
                    upload.getId(),
                    "memory://lecture-uploads/%s/%d".formatted(upload.getPublicId(), i + 1),
                    file.getFileName(),
                    (long) file.getBytes().length,
                    trimFileType(file.getContentType()),
                    i + 1,
                    uploadType == StudyMaterialUploadType.IMAGE ? "pending" : null
            ));
        }
        lectureUploadFileRepository.saveAll(uploadFiles);
    }

    private String trimFileType(String contentType) {
        String resolved = StringUtils.hasText(contentType) ? contentType : "application/octet";
        return resolved.length() <= 20 ? resolved : resolved.substring(0, 20);
    }

    private void saveProcessingJob(Long lectureUploadId, Long userId) {
        lectureProcessingJobRepository.save(LectureProcessingJob.create(lectureUploadId, userId, ESTIMATED_SECONDS));
    }
}
