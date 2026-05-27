package com.f1.quiket.domain.lecture.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.lecture.dto.LectureMaterialAiProcessResult;
import com.f1.quiket.domain.lecture.dto.LecturePartDraft;
import com.f1.quiket.domain.lecture.dto.PartSplitMethod;
import com.f1.quiket.domain.lecture.entity.LectureProcessingJob;
import com.f1.quiket.domain.lecture.entity.LectureUpload;
import com.f1.quiket.domain.lecture.event.LectureUploadProcessingRequestedEvent;
import com.f1.quiket.domain.lecture.repository.LectureProcessingJobRepository;
import com.f1.quiket.domain.lecture.repository.LectureUploadFileRepository;
import com.f1.quiket.domain.lecture.repository.LectureUploadRepository;
import com.f1.quiket.domain.material.dto.StudyMaterialUploadType;
import com.f1.quiket.domain.part.entity.Part;
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

class LectureUploadProcessingServiceTest {

    private LectureUploadRepository lectureUploadRepository;
    private LectureUploadFileRepository lectureUploadFileRepository;
    private LectureProcessingJobRepository lectureProcessingJobRepository;
    private PartRepository partRepository;
    private LectureMaterialAiProcessor lectureMaterialAiProcessor;
    private LectureUploadProcessingService service;

    @BeforeEach
    void setUp() {
        lectureUploadRepository = mock(LectureUploadRepository.class);
        lectureUploadFileRepository = mock(LectureUploadFileRepository.class);
        lectureProcessingJobRepository = mock(LectureProcessingJobRepository.class);
        partRepository = mock(PartRepository.class);
        lectureMaterialAiProcessor = mock(LectureMaterialAiProcessor.class);
        service = new LectureUploadProcessingService(
                lectureUploadRepository,
                lectureUploadFileRepository,
                lectureProcessingJobRepository,
                partRepository,
                lectureMaterialAiProcessor,
                transactionManager()
        );
    }

    @Test
    void process_saves_parts_and_marks_upload_completed() {
        LectureUpload upload = LectureUpload.create(20L, 1L, StudyMaterialUploadType.TEXT, PartSplitMethod.AUTO);
        ReflectionTestUtils.setField(upload, "id", 30L);
        LectureProcessingJob processingJob = LectureProcessingJob.create(upload.getId(), 1L, 30);
        when(lectureUploadRepository.findById(30L)).thenReturn(Optional.of(upload));
        when(lectureProcessingJobRepository.findByLectureUploadId(upload.getId())).thenReturn(Optional.of(processingJob));
        when(lectureMaterialAiProcessor.process(any()))
                .thenReturn(LectureMaterialAiProcessResult.builder()
                        .provider("groq")
                        .extractedText("원문 텍스트")
                        .parts(List.of(LecturePartDraft.builder()
                                .partNumber(1)
                                .name("개념")
                                .content("원문 텍스트")
                                .build()))
                        .build());

        service.process(new LectureUploadProcessingRequestedEvent(
                30L,
                20L,
                10L,
                1L,
                "1장",
                StudyMaterialUploadType.TEXT,
                PartSplitMethod.AUTO,
                "원문 텍스트",
                List.of(),
                List.of()
        ));

        ArgumentCaptor<List<Part>> partsCaptor = ArgumentCaptor.forClass(List.class);
        verify(partRepository).saveAll(partsCaptor.capture());
        assertThat(partsCaptor.getValue()).hasSize(1);
        assertThat(upload.getStatus()).isEqualTo("completed");
        assertThat(upload.getRawText()).isEqualTo("원문 텍스트");
        assertThat(processingJob.getStatus()).isEqualTo("completed");
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
