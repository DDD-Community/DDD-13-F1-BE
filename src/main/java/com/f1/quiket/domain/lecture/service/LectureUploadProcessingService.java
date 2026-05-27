package com.f1.quiket.domain.lecture.service;

import com.f1.quiket.domain.lecture.dto.LectureMaterialAiProcessRequest;
import com.f1.quiket.domain.lecture.dto.LectureMaterialAiProcessResult;
import com.f1.quiket.domain.lecture.dto.LecturePartDraft;
import com.f1.quiket.domain.lecture.entity.LectureUpload;
import com.f1.quiket.domain.lecture.entity.LectureUploadFile;
import com.f1.quiket.domain.lecture.event.LectureUploadProcessingRequestedEvent;
import com.f1.quiket.domain.lecture.repository.LectureUploadFileRepository;
import com.f1.quiket.domain.lecture.repository.LectureUploadRepository;
import com.f1.quiket.domain.material.dto.StudyMaterialUploadType;
import com.f1.quiket.domain.part.entity.Part;
import com.f1.quiket.domain.part.repository.PartRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.util.List;
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
 * 강의 업로드 비동기 처리 서비스
 *
 * OCR/텍스트 추출/파트 분류 유틸 호출 후 raw_text와 초기 parts 저장을 담당
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LectureUploadProcessingService {

    private static final int MAX_PART_CONTENT_LENGTH = 30000;

    private final LectureUploadRepository lectureUploadRepository;
    private final LectureUploadFileRepository lectureUploadFileRepository;
    private final PartRepository partRepository;
    private final LectureMaterialAiProcessor lectureMaterialAiProcessor;
    private final PlatformTransactionManager transactionManager;

    /**
     * 업로드 접수 커밋 이후 비동기 처리 시작
     *
     * @param event 업로드 처리 요청 이벤트
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(LectureUploadProcessingRequestedEvent event) {
        process(event);
    }

    /**
     * OCR/텍스트 추출/파트 분류 처리
     *
     * @param event 업로드 처리 요청 이벤트
     */
    public void process(LectureUploadProcessingRequestedEvent event) {
        try {
            markProcessing(event.lectureUploadId());

            // Tika/Gemini/Groq 기반 처리 유틸 호출
            LectureMaterialAiProcessResult result = lectureMaterialAiProcessor.process(
                    LectureMaterialAiProcessRequest.builder()
                            .uploadType(event.uploadType())
                            .partSplitMethod(event.partSplitMethod())
                            .chapterName(event.chapterName())
                            .text(event.text())
                            .files(event.files())
                            .partSplitPlans(event.partSplitPlans())
                            .build()
            );

            saveCompleted(event, result);
        } catch (Exception e) {
            log.warn("Lecture upload processing failed. lectureUploadId={}", event.lectureUploadId(), e);
            markFailed(event, failMessage(e));
        }
    }

    private void markProcessing(Long lectureUploadId) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            LectureUpload upload = getUpload(lectureUploadId);
            // lecture_uploads.status=processing
            upload.markProcessing();
        });
    }

    private void saveCompleted(LectureUploadProcessingRequestedEvent event, LectureMaterialAiProcessResult result) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            LectureUpload upload = getUpload(event.lectureUploadId());
            String rawText = resolveRawText(result, event);
            List<Part> parts = createParts(event, upload, result.getParts());
            partRepository.saveAll(parts);

            if (event.uploadType() == StudyMaterialUploadType.IMAGE) {
                // 이미지 OCR 성공 상태 반영
                lectureUploadFileRepository.findAllByLectureUploadIdAndDeletedAtIsNullOrderByDisplayOrderAsc(upload.getId())
                        .forEach(LectureUploadFile::markOcrSuccess);
            }

            // lecture_uploads.status=completed
            upload.markCompleted(rawText);
        });
    }

    private List<Part> createParts(
            LectureUploadProcessingRequestedEvent event,
            LectureUpload upload,
            List<LecturePartDraft> drafts
    ) {
        if (drafts == null || drafts.isEmpty()) {
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, "AI 응답에 파트 정보가 없습니다.");
        }
        // parts 저장
        return drafts.stream()
                .map(draft -> {
                    validateDraft(draft);
                    return Part.createFromLectureUpload(
                            event.chapterId(),
                            event.subjectId(),
                            event.userId(),
                            upload.getId(),
                            draft.getName().trim(),
                            draft.getPartNumber(),
                            draft.getContent().trim()
                    );
                })
                .toList();
    }

    private void validateDraft(LecturePartDraft draft) {
        if (draft == null
                || draft.getPartNumber() == null
                || draft.getPartNumber() < 1
                || !StringUtils.hasText(draft.getName())
                || !StringUtils.hasText(draft.getContent())) {
            throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE, "AI 응답 파트 형식이 올바르지 않습니다.");
        }
        if (draft.getContent().length() > MAX_PART_CONTENT_LENGTH) {
            throw new CustomException(ErrorCode.UNPROCESSABLE_ENTITY, "파트 본문은 30,000자를 초과할 수 없습니다.");
        }
    }

    private String resolveRawText(LectureMaterialAiProcessResult result, LectureUploadProcessingRequestedEvent event) {
        if (StringUtils.hasText(result.getExtractedText())) {
            return result.getExtractedText();
        }
        if (event.uploadType() == StudyMaterialUploadType.IMAGE || event.uploadType() == StudyMaterialUploadType.PDF) {
            return result.getParts().stream()
                    .map(LecturePartDraft::getContent)
                    .filter(StringUtils::hasText)
                    .reduce((left, right) -> left + "\n\n" + right)
                    .orElse(null);
        }
        return event.text();
    }

    private void markFailed(LectureUploadProcessingRequestedEvent event, String failReason) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            LectureUpload upload = getUpload(event.lectureUploadId());
            // OCR 전체 실패/AI 실패 처리
            upload.markFailed(failReason);
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

    private String failMessage(Exception e) {
        if (e instanceof CustomException customException && StringUtils.hasText(customException.getMessage())) {
            return customException.getMessage();
        }
        return "업로드에 실패하였습니다. 다시 시도해주세요";
    }
}
