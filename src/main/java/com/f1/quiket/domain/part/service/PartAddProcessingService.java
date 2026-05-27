package com.f1.quiket.domain.part.service;

import com.f1.quiket.domain.lecture.entity.LectureProcessingJob;
import com.f1.quiket.domain.lecture.entity.LectureUpload;
import com.f1.quiket.domain.lecture.entity.LectureUploadFile;
import com.f1.quiket.domain.lecture.repository.LectureProcessingJobRepository;
import com.f1.quiket.domain.lecture.repository.LectureUploadFileRepository;
import com.f1.quiket.domain.lecture.repository.LectureUploadRepository;
import com.f1.quiket.domain.material.dto.StudyMaterialTextExtractionRequest;
import com.f1.quiket.domain.material.dto.StudyMaterialTextExtractionResult;
import com.f1.quiket.domain.material.dto.StudyMaterialUploadType;
import com.f1.quiket.domain.material.processor.StudyMaterialTextExtractor;
import com.f1.quiket.domain.part.entity.Part;
import com.f1.quiket.domain.part.event.PartAddProcessingRequestedEvent;
import com.f1.quiket.domain.part.repository.PartRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 * 기존 챕터 파트 추가 비동기 처리 서비스
 *
 * 텍스트 직접 입력, PDF 텍스트 추출, 이미지 OCR 결과를 단일 파트로 저장
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PartAddProcessingService {

    private static final int MAX_PART_CONTENT_LENGTH = 30000;
    private static final int ESTIMATED_SECONDS = 30;

    private final LectureUploadRepository lectureUploadRepository;
    private final LectureUploadFileRepository lectureUploadFileRepository;
    private final LectureProcessingJobRepository lectureProcessingJobRepository;
    private final PartRepository partRepository;
    private final StudyMaterialTextExtractor studyMaterialTextExtractor;
    private final PlatformTransactionManager transactionManager;

    /**
     * 파트 추가 접수 커밋 이후 비동기 처리 시작
     *
     * @param event 파트 추가 처리 요청 이벤트
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PartAddProcessingRequestedEvent event) {
        process(event);
    }

    /**
     * OCR/텍스트 추출 후 단일 파트 저장
     *
     * @param event 파트 추가 처리 요청 이벤트
     */
    public void process(PartAddProcessingRequestedEvent event) {
        try {
            markProcessing(event.lectureUploadId());

            // PDF 텍스트 추출 또는 Gemini OCR 호출
            StudyMaterialTextExtractionResult result = studyMaterialTextExtractor.extract(
                    StudyMaterialTextExtractionRequest.builder()
                            .uploadType(event.uploadType())
                            .text(event.text())
                            .files(event.files())
                            .build()
            );

            saveCompleted(event, result);
        } catch (Exception e) {
            log.warn("Part add processing failed. lectureUploadId={}", event.lectureUploadId(), e);
            markFailed(event, failMessage(e));
        }
    }

    private void markProcessing(Long lectureUploadId) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            LectureUpload upload = getUpload(lectureUploadId);
            // lecture_uploads.status=processing
            upload.markProcessing();
            getOrCreateJob(upload).markInProgress();
        });
    }

    private void saveCompleted(PartAddProcessingRequestedEvent event, StudyMaterialTextExtractionResult result) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            LectureUpload upload = getUpload(event.lectureUploadId());
            String content = normalizeExtractedText(result.getExtractedText());

            // parts 저장
            partRepository.save(Part.createFromLectureUpload(
                    event.chapterId(),
                    event.subjectId(),
                    event.userId(),
                    upload.getId(),
                    event.partName(),
                    event.partNumber(),
                    content
            ));

            if (event.uploadType() == StudyMaterialUploadType.IMAGE) {
                updateImageOcrStatuses(upload, result);
            }

            // lecture_uploads.status=completed
            upload.markCompleted(content);
            getOrCreateJob(upload).markCompleted();
        });
    }

    private void updateImageOcrStatuses(LectureUpload upload, StudyMaterialTextExtractionResult result) {
        Set<Integer> failedDisplayOrders = new HashSet<>(
                result.getFailedDisplayOrders() == null ? Set.of() : result.getFailedDisplayOrders()
        );
        // 이미지 OCR 성공/실패 상태 반영
        lectureUploadFileRepository.findAllByLectureUploadIdAndDeletedAtIsNullOrderByDisplayOrderAsc(upload.getId())
                .forEach(file -> {
                    if (failedDisplayOrders.contains(file.getDisplayOrder())) {
                        file.markOcrFailed();
                        return;
                    }
                    file.markOcrSuccess();
                });
    }

    private String normalizeExtractedText(String extractedText) {
        if (!StringUtils.hasText(extractedText)) {
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, "이미지를 인식하지 못했어요. 다른 이미지를 시도해주세요");
        }
        String normalized = extractedText.trim();
        if (normalized.length() > MAX_PART_CONTENT_LENGTH) {
            throw new CustomException(
                    ErrorCode.UNPROCESSABLE_ENTITY,
                    "파트 내용은 30,000자 이하로 저장할 수 있어요. 더 작은 범위로 나누어 업로드해주세요"
            );
        }
        return normalized;
    }

    private void markFailed(PartAddProcessingRequestedEvent event, String failReason) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            LectureUpload upload = getUpload(event.lectureUploadId());
            // OCR 전체 실패/추출 실패 처리
            upload.markFailed();
            getOrCreateJob(upload).markFailed("processing_failed", failReason);
            if (event.uploadType() == StudyMaterialUploadType.IMAGE) {
                lectureUploadFileRepository.findAllByLectureUploadIdAndDeletedAtIsNullOrderByDisplayOrderAsc(upload.getId())
                        .forEach(LectureUploadFile::markOcrFailed);
            }
        });
    }

    private LectureUpload getUpload(Long lectureUploadId) {
        return lectureUploadRepository.findById(lectureUploadId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
    }

    private LectureProcessingJob getOrCreateJob(LectureUpload upload) {
        return lectureProcessingJobRepository.findByLectureUploadId(upload.getId())
                .orElseGet(() -> lectureProcessingJobRepository.save(
                        LectureProcessingJob.create(upload.getId(), upload.getUserId(), ESTIMATED_SECONDS)
                ));
    }

    private String failMessage(Exception e) {
        if (e instanceof CustomException customException && StringUtils.hasText(customException.getMessage())) {
            return customException.getMessage();
        }
        return "업로드에 실패했어요. 다시 시도해주세요";
    }
}
