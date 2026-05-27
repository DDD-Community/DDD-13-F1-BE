package com.f1.quiket.domain.lecture.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.f1.quiket.domain.chapter.entity.Chapter;
import com.f1.quiket.domain.chapter.repository.ChapterRepository;
import com.f1.quiket.domain.lecture.dto.LecturePartSplitPlan;
import com.f1.quiket.domain.lecture.dto.LectureTextUploadRequest;
import com.f1.quiket.domain.lecture.dto.LectureUploadAcceptedResponse;
import com.f1.quiket.domain.lecture.dto.PartSplitMethod;
import com.f1.quiket.domain.lecture.dto.PartSplitPlanRequest;
import com.f1.quiket.domain.lecture.entity.LectureProcessingJob;
import com.f1.quiket.domain.lecture.entity.LectureUpload;
import com.f1.quiket.domain.lecture.entity.LectureUploadFile;
import com.f1.quiket.domain.lecture.entity.PartSplitPlan;
import com.f1.quiket.domain.lecture.event.LectureUploadProcessingRequestedEvent;
import com.f1.quiket.domain.lecture.repository.LectureProcessingJobRepository;
import com.f1.quiket.domain.lecture.repository.LectureUploadFileRepository;
import com.f1.quiket.domain.lecture.repository.LectureUploadRepository;
import com.f1.quiket.domain.lecture.repository.PartSplitPlanRepository;
import com.f1.quiket.domain.material.dto.StudyMaterialFile;
import com.f1.quiket.domain.material.dto.StudyMaterialUploadType;
import com.f1.quiket.domain.subject.entity.Subject;
import com.f1.quiket.domain.subject.repository.SubjectRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 강의 업로드 접수 서비스
 *
 * 요청 검증, 챕터/업로드/파일/직접 분류 계획 저장, 처리 이벤트 발행을 담당
 */
@Service
@RequiredArgsConstructor
public class LectureUploadCreateService {

    private static final long MAX_UPLOAD_SIZE_BYTES = 50L * 1024L * 1024L;
    private static final int MIN_TEXT_LENGTH = 100;
    private static final int MAX_TEXT_LENGTH = 30000;
    private static final int ESTIMATED_SECONDS = 30;

    private final SubjectRepository subjectRepository;
    private final ChapterRepository chapterRepository;
    private final LectureUploadRepository lectureUploadRepository;
    private final LectureUploadFileRepository lectureUploadFileRepository;
    private final LectureProcessingJobRepository lectureProcessingJobRepository;
    private final PartSplitPlanRepository partSplitPlanRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 텍스트 직접 입력 업로드 접수
     *
     * @param userId 인증 사용자 내부 식별자
     * @param request 텍스트 업로드 요청
     * @return 업로드 접수 응답
     */
    @Transactional
    public LectureUploadAcceptedResponse createTextUpload(Long userId, LectureTextUploadRequest request) {
        StudyMaterialUploadType uploadType = parseUploadType(request.getUploadType());
        PartSplitMethod partSplitMethod = parsePartSplitMethod(request.getPartSplitMethod());
        validateTextUpload(uploadType, request.getText());

        List<LecturePartSplitPlan> plans = validateAndConvertPlans(partSplitMethod, request.getPartSplitPlans());
        Subject subject = getOwnedSubject(userId, request.getSubjectId());
        Chapter chapter = createChapter(subject, userId, request.getChapterName());
        LectureUpload upload = lectureUploadRepository.save(
                LectureUpload.create(chapter.getId(), userId, uploadType, partSplitMethod)
        );
        saveProcessingJob(upload.getId(), userId);
        savePartSplitPlans(upload.getId(), plans);

        eventPublisher.publishEvent(new LectureUploadProcessingRequestedEvent(
                upload.getId(),
                chapter.getId(),
                subject.getId(),
                userId,
                chapter.getName(),
                uploadType,
                partSplitMethod,
                request.getText(),
                List.of(),
                plans
        ));

        return LectureUploadAcceptedResponse.of(upload, subject, chapter);
    }

    /**
     * PDF 또는 이미지 파일 업로드 접수
     *
     * @param userId 인증 사용자 내부 식별자
     * @param subjectPublicId 과목 공개 식별자
     * @param chapterName 생성할 챕터명
     * @param uploadTypeValue 업로드 타입 문자열
     * @param partSplitMethodValue 파트 분류 방식 문자열
     * @param multipartFiles 업로드 파일 목록
     * @param partSplitPlansJson 직접 분류 계획 JSON 문자열
     * @return 업로드 접수 응답
     */
    @Transactional
    public LectureUploadAcceptedResponse createFileUpload(
            Long userId,
            String subjectPublicId,
            String chapterName,
            String uploadTypeValue,
            String partSplitMethodValue,
            List<MultipartFile> multipartFiles,
            String partSplitPlansJson
    ) {
        StudyMaterialUploadType uploadType = parseUploadType(uploadTypeValue);
        PartSplitMethod partSplitMethod = parsePartSplitMethod(partSplitMethodValue);
        validateChapterName(chapterName);
        List<StudyMaterialFile> files = validateAndReadFiles(uploadType, multipartFiles);
        List<LecturePartSplitPlan> plans = validateAndConvertPlans(partSplitMethod, parsePlansJson(partSplitPlansJson));

        Subject subject = getOwnedSubject(userId, subjectPublicId);
        Chapter chapter = createChapter(subject, userId, chapterName);
        LectureUpload upload = lectureUploadRepository.save(
                LectureUpload.create(chapter.getId(), userId, uploadType, partSplitMethod)
        );
        saveProcessingJob(upload.getId(), userId);
        saveUploadFiles(upload, uploadType, files);
        savePartSplitPlans(upload.getId(), plans);

        eventPublisher.publishEvent(new LectureUploadProcessingRequestedEvent(
                upload.getId(),
                chapter.getId(),
                subject.getId(),
                userId,
                chapter.getName(),
                uploadType,
                partSplitMethod,
                null,
                files,
                plans
        ));

        return LectureUploadAcceptedResponse.of(upload, subject, chapter);
    }

    private Subject getOwnedSubject(Long userId, String subjectPublicId) {
        if (!StringUtils.hasText(subjectPublicId)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "subjectId는 필수입니다.");
        }
        // 과목 소유권 검증
        return subjectRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(subjectPublicId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.SUBJECT_NOT_FOUND));
    }

    private Chapter createChapter(Subject subject, Long userId, String chapterName) {
        validateChapterName(chapterName);
        int nextDisplayOrder = chapterRepository.findMaxDisplayOrderBySubjectId(subject.getId()) + 1;
        return chapterRepository.save(Chapter.create(subject.getId(), userId, chapterName.trim(), nextDisplayOrder));
    }

    private StudyMaterialUploadType parseUploadType(String value) {
        try {
            return StudyMaterialUploadType.from(value);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "지원하지 않는 업로드 타입입니다.");
        }
    }

    private PartSplitMethod parsePartSplitMethod(String value) {
        try {
            return PartSplitMethod.from(value);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "지원하지 않는 파트 분류 방식입니다.");
        }
    }

    private void validateTextUpload(StudyMaterialUploadType uploadType, String text) {
        if (uploadType != StudyMaterialUploadType.TEXT) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "텍스트 업로드는 uploadType=text만 사용할 수 있습니다.");
        }
        if (!StringUtils.hasText(text) || text.length() < MIN_TEXT_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "100자 이상 입력해야 퀴즈를 만들 수 있어요");
        }
        if (text.length() > MAX_TEXT_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "text는 30,000자 이하로 입력해주세요.");
        }
    }

    private void validateChapterName(String chapterName) {
        if (!StringUtils.hasText(chapterName) || chapterName.trim().length() > 30) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "chapterName은 1~30자로 입력해주세요.");
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
                throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, "업로드에 실패하였습니다. 다시 시도해주세요", e);
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

    private List<PartSplitPlanRequest> parsePlansJson(String partSplitPlansJson) {
        if (!StringUtils.hasText(partSplitPlansJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(partSplitPlansJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "partSplitPlansJson 형식이 올바르지 않습니다.");
        }
    }

    private List<LecturePartSplitPlan> validateAndConvertPlans(
            PartSplitMethod partSplitMethod,
            List<PartSplitPlanRequest> requests
    ) {
        if (partSplitMethod == PartSplitMethod.AUTO) {
            return List.of();
        }
        if (requests == null || requests.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "직접 분류 방식은 partSplitPlans가 필요합니다.");
        }

        Set<Integer> partNumbers = new HashSet<>();
        List<LecturePartSplitPlan> plans = new ArrayList<>();
        for (PartSplitPlanRequest request : requests) {
            if (request == null || request.getPartNumber() == null || request.getPartNumber() < 1) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "partNumber는 1 이상이어야 합니다.");
            }
            if (!partNumbers.add(request.getPartNumber())) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "partNumber는 중복될 수 없습니다.");
            }
            plans.add(LecturePartSplitPlan.builder()
                    .partNumber(request.getPartNumber())
                    .intendedName(normalizeOptional(request.getIntendedName()))
                    .build());
        }
        return plans;
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
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

    private void savePartSplitPlans(Long lectureUploadId, List<LecturePartSplitPlan> plans) {
        if (plans.isEmpty()) {
            return;
        }
        // 직접 분류 계획 저장
        partSplitPlanRepository.saveAll(plans.stream()
                .map(plan -> PartSplitPlan.create(
                        lectureUploadId,
                        plan.getPartNumber(),
                        plan.getIntendedName()
                ))
                .toList());
    }

    private void saveProcessingJob(Long lectureUploadId, Long userId) {
        lectureProcessingJobRepository.save(LectureProcessingJob.create(lectureUploadId, userId, ESTIMATED_SECONDS));
    }
}
