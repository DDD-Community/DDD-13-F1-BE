package com.f1.quiket.domain.part.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.lecture.dto.PartSplitMethod;
import com.f1.quiket.domain.lecture.entity.LectureProcessingJob;
import com.f1.quiket.domain.lecture.entity.LectureUpload;
import com.f1.quiket.domain.lecture.entity.LectureUploadFile;
import com.f1.quiket.domain.lecture.repository.LectureProcessingJobRepository;
import com.f1.quiket.domain.lecture.repository.LectureUploadFileRepository;
import com.f1.quiket.domain.lecture.repository.LectureUploadRepository;
import com.f1.quiket.domain.material.dto.StudyMaterialTextExtractionResult;
import com.f1.quiket.domain.material.dto.StudyMaterialUploadType;
import com.f1.quiket.domain.material.processor.StudyMaterialTextExtractor;
import com.f1.quiket.domain.part.entity.Part;
import com.f1.quiket.domain.part.event.PartAddProcessingRequestedEvent;
import com.f1.quiket.domain.part.repository.PartRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

class PartAddProcessingServiceTest {

    private LectureUploadRepository lectureUploadRepository;
    private LectureUploadFileRepository lectureUploadFileRepository;
    private LectureProcessingJobRepository lectureProcessingJobRepository;
    private PartRepository partRepository;
    private StudyMaterialTextExtractor studyMaterialTextExtractor;
    private PartAddProcessingService service;

    @BeforeEach
    void setUp() {
        lectureUploadRepository = mock(LectureUploadRepository.class);
        lectureUploadFileRepository = mock(LectureUploadFileRepository.class);
        lectureProcessingJobRepository = mock(LectureProcessingJobRepository.class);
        partRepository = mock(PartRepository.class);
        studyMaterialTextExtractor = mock(StudyMaterialTextExtractor.class);
        service = new PartAddProcessingService(
                lectureUploadRepository,
                lectureUploadFileRepository,
                lectureProcessingJobRepository,
                partRepository,
                studyMaterialTextExtractor,
                transactionManager()
        );
    }

    @Test
    void process_extracts_text_and_saves_single_part() {
        LectureUpload upload = LectureUpload.create(20L, 1L, StudyMaterialUploadType.TEXT, PartSplitMethod.MANUAL);
        ReflectionTestUtils.setField(upload, "id", 30L);
        LectureProcessingJob processingJob = LectureProcessingJob.create(upload.getId(), 1L, 30);
        when(lectureUploadRepository.findById(30L)).thenReturn(Optional.of(upload));
        when(lectureProcessingJobRepository.findByLectureUploadId(upload.getId())).thenReturn(Optional.of(processingJob));
        when(studyMaterialTextExtractor.extract(any()))
                .thenReturn(StudyMaterialTextExtractionResult.builder()
                        .provider("none")
                        .extractedText(" 추출된 본문 ")
                        .build());

        service.process(new PartAddProcessingRequestedEvent(
                30L,
                20L,
                10L,
                1L,
                "새 파트",
                4,
                StudyMaterialUploadType.TEXT,
                "추출된 본문",
                List.of()
        ));

        ArgumentCaptor<Part> partCaptor = ArgumentCaptor.forClass(Part.class);
        verify(partRepository).save(partCaptor.capture());
        assertThat(partCaptor.getValue().getPartNumber()).isEqualTo(4);
        assertThat(partCaptor.getValue().getName()).isEqualTo("새 파트");
        assertThat(partCaptor.getValue().getContent()).isEqualTo("추출된 본문");
        assertThat(upload.getStatus()).isEqualTo("completed");
        assertThat(upload.getRawText()).isEqualTo("추출된 본문");
        assertThat(processingJob.getStatus()).isEqualTo("completed");
    }

    @Test
    void process_marks_partial_image_ocr_failure_on_upload_files() {
        LectureUpload upload = LectureUpload.create(20L, 1L, StudyMaterialUploadType.IMAGE, PartSplitMethod.MANUAL);
        ReflectionTestUtils.setField(upload, "id", 30L);
        LectureProcessingJob processingJob = LectureProcessingJob.create(upload.getId(), 1L, 30);
        LectureUploadFile successFile = uploadFile(upload.getId(), 1);
        LectureUploadFile failedFile = uploadFile(upload.getId(), 2);
        when(lectureUploadRepository.findById(30L)).thenReturn(Optional.of(upload));
        when(lectureProcessingJobRepository.findByLectureUploadId(upload.getId())).thenReturn(Optional.of(processingJob));
        when(lectureUploadFileRepository.findAllByLectureUploadIdAndDeletedAtIsNullOrderByDisplayOrderAsc(upload.getId()))
                .thenReturn(List.of(successFile, failedFile));
        when(studyMaterialTextExtractor.extract(any()))
                .thenReturn(StudyMaterialTextExtractionResult.builder()
                        .provider("gemini")
                        .extractedText("인식된 본문")
                        .failedDisplayOrders(List.of(2))
                        .build());

        service.process(new PartAddProcessingRequestedEvent(
                30L,
                20L,
                10L,
                1L,
                "새 파트",
                4,
                StudyMaterialUploadType.IMAGE,
                null,
                List.of()
        ));

        assertThat(successFile.getOcrStatus()).isEqualTo("success");
        assertThat(failedFile.getOcrStatus()).isEqualTo("failed");
        assertThat(upload.getStatus()).isEqualTo("completed");
    }

    private LectureUploadFile uploadFile(Long lectureUploadId, int displayOrder) {
        return LectureUploadFile.create(
                lectureUploadId,
                "memory://test/%d".formatted(displayOrder),
                "slide%d.png".formatted(displayOrder),
                10L,
                "image/png",
                displayOrder,
                "pending"
        );
    }

    private PlatformTransactionManager transactionManager() {
        return new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
            }

            @Override
            public void rollback(TransactionStatus status) {
            }
        };
    }
}
