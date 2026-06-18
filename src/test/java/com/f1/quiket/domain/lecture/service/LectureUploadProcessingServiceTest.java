package com.f1.quiket.domain.lecture.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.chapter.entity.Chapter;
import com.f1.quiket.domain.chapter.repository.ChapterRepository;
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
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
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
    private ChapterRepository chapterRepository;
    private LectureMaterialAiProcessor lectureMaterialAiProcessor;
    private LectureUploadProcessingService service;

    @BeforeEach
    void setUp() {
        lectureUploadRepository = mock(LectureUploadRepository.class);
        lectureUploadFileRepository = mock(LectureUploadFileRepository.class);
        lectureProcessingJobRepository = mock(LectureProcessingJobRepository.class);
        partRepository = mock(PartRepository.class);
        chapterRepository = mock(ChapterRepository.class);
        lectureMaterialAiProcessor = mock(LectureMaterialAiProcessor.class);
        service = new LectureUploadProcessingService(
                lectureUploadRepository,
                lectureUploadFileRepository,
                lectureProcessingJobRepository,
                partRepository,
                chapterRepository,
                lectureMaterialAiProcessor,
                transactionManager()
        );
    }

    /**
     * 기본 처리 흐름: 파트 저장 및 업로드 상태 completed 전환 검증
     * chapterName을 직접 입력한 경우 챕터명 업데이트가 발생하지 않음을 함께 검증
     */
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

        // chapterName을 직접 입력한 이벤트
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
        // 사용자가 챕터명을 직접 입력한 경우 chapterRepository 호출 없음
        verify(chapterRepository, never()).findById(any());
    }

    /**
     * chapterName 미지정 시 AI 생성 챕터명으로 chapter.name 업데이트 검증
     */
    @Test
    void process_updates_chapter_name_when_chapterName_is_null_and_ai_generates_it() {
        LectureUpload upload = LectureUpload.create(20L, 1L, StudyMaterialUploadType.TEXT, PartSplitMethod.AUTO);
        ReflectionTestUtils.setField(upload, "id", 30L);
        LectureProcessingJob processingJob = LectureProcessingJob.create(upload.getId(), 1L, 30);

        Chapter chapter = Chapter.create(10L, 1L, "새 챕터", 1);
        ReflectionTestUtils.setField(chapter, "id", 20L);

        when(lectureUploadRepository.findById(30L)).thenReturn(Optional.of(upload));
        when(lectureProcessingJobRepository.findByLectureUploadId(upload.getId())).thenReturn(Optional.of(processingJob));
        when(chapterRepository.findById(20L)).thenReturn(Optional.of(chapter));
        when(lectureMaterialAiProcessor.process(any()))
                .thenReturn(LectureMaterialAiProcessResult.builder()
                        .provider("groq")
                        .extractedText("원문 텍스트")
                        .parts(List.of(LecturePartDraft.builder()
                                .partNumber(1)
                                .name("개념")
                                .content("원문 텍스트")
                                .build()))
                        // AI가 생성한 챕터명
                        .generatedChapterName("데이터베이스 모델링")
                        .build());

        // chapterName이 null인 이벤트 (미지정)
        service.process(new LectureUploadProcessingRequestedEvent(
                30L,
                20L,
                10L,
                1L,
                null,
                StudyMaterialUploadType.TEXT,
                PartSplitMethod.AUTO,
                "원문 텍스트",
                List.of(),
                List.of()
        ));

        // AI 생성 챕터명으로 chapter.name이 업데이트됨
        assertThat(chapter.getName()).isEqualTo("데이터베이스 모델링");
        assertThat(upload.getStatus()).isEqualTo("completed");
        assertThat(processingJob.getStatus()).isEqualTo("completed");
    }

    /**
     * chapterName 미지정이지만 AI가 챕터명을 생성하지 못한 경우
     * 임시명("새 챕터") 유지 및 chapter 업데이트 미발생 검증
     */
    @Test
    void process_keeps_placeholder_chapter_name_when_ai_fails_to_generate() {
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
                        // AI 챕터명 생성 실패 (null)
                        .generatedChapterName(null)
                        .build());

        service.process(new LectureUploadProcessingRequestedEvent(
                30L,
                20L,
                10L,
                1L,
                null,
                StudyMaterialUploadType.TEXT,
                PartSplitMethod.AUTO,
                "원문 텍스트",
                List.of(),
                List.of()
        ));

        assertThat(upload.getStatus()).isEqualTo("completed");
        // AI 챕터명 미생성 시 chapterRepository 조회 없음 (임시명 유지)
        verify(chapterRepository, never()).findById(any());
    }

    /**
     * AI 응답 파싱 실패 시 fail_code=AI_ERROR, fail_reason=상세 원인 저장 검증
     */
    @Test
    void process_marks_failed_with_AI_ERROR_when_ai_parsing_fails() {
        LectureUpload upload = LectureUpload.create(20L, 1L, StudyMaterialUploadType.TEXT, PartSplitMethod.AUTO);
        ReflectionTestUtils.setField(upload, "id", 30L);
        LectureProcessingJob processingJob = LectureProcessingJob.create(upload.getId(), 1L, 30);
        when(lectureUploadRepository.findById(30L)).thenReturn(Optional.of(upload));
        when(lectureProcessingJobRepository.findByLectureUploadId(upload.getId())).thenReturn(Optional.of(processingJob));
        when(lectureMaterialAiProcessor.process(any()))
                .thenThrow(new CustomException(ErrorCode.LECTURE_AI_ERROR, "AI 응답 파싱에 실패했습니다."));

        service.process(new LectureUploadProcessingRequestedEvent(
                30L, 20L, 10L, 1L, "1장", StudyMaterialUploadType.TEXT,
                PartSplitMethod.AUTO, "원문 텍스트", List.of(), List.of()
        ));

        assertThat(upload.getStatus()).isEqualTo("failed");
        assertThat(processingJob.getFailCode()).isEqualTo(ErrorCode.LECTURE_AI_ERROR.name());
        assertThat(processingJob.getFailReason()).isEqualTo("AI 응답 파싱에 실패했습니다.");
    }

    /**
     * PDF 텍스트 추출 실패 시 fail_code=CONTENT_ERROR 저장 검증
     */
    @Test
    void process_marks_failed_with_CONTENT_ERROR_when_pdf_extraction_fails() {
        LectureUpload upload = LectureUpload.create(20L, 1L, StudyMaterialUploadType.PDF, PartSplitMethod.AUTO);
        ReflectionTestUtils.setField(upload, "id", 30L);
        LectureProcessingJob processingJob = LectureProcessingJob.create(upload.getId(), 1L, 30);
        when(lectureUploadRepository.findById(30L)).thenReturn(Optional.of(upload));
        when(lectureProcessingJobRepository.findByLectureUploadId(upload.getId())).thenReturn(Optional.of(processingJob));
        when(lectureMaterialAiProcessor.process(any()))
                .thenThrow(new CustomException(ErrorCode.LECTURE_CONTENT_ERROR, "PDF 텍스트 추출에 실패했습니다."));

        service.process(new LectureUploadProcessingRequestedEvent(
                30L, 20L, 10L, 1L, "1장", StudyMaterialUploadType.PDF,
                PartSplitMethod.AUTO, null, List.of(), List.of()
        ));

        assertThat(upload.getStatus()).isEqualTo("failed");
        assertThat(processingJob.getFailCode()).isEqualTo(ErrorCode.LECTURE_CONTENT_ERROR.name());
    }

    /**
     * 파트 본문 30,000자 초과 시 fail_code=LECTURE_CONTENT_ERROR 저장 검증
     */
    @Test
    void process_marks_failed_with_CONTENT_ERROR_when_part_content_exceeds_limit() {
        LectureUpload upload = LectureUpload.create(20L, 1L, StudyMaterialUploadType.TEXT, PartSplitMethod.AUTO);
        ReflectionTestUtils.setField(upload, "id", 30L);
        LectureProcessingJob processingJob = LectureProcessingJob.create(upload.getId(), 1L, 30);
        when(lectureUploadRepository.findById(30L)).thenReturn(Optional.of(upload));
        when(lectureProcessingJobRepository.findByLectureUploadId(upload.getId())).thenReturn(Optional.of(processingJob));
        String oversizedContent = "가".repeat(30001);
        when(lectureMaterialAiProcessor.process(any()))
                .thenReturn(LectureMaterialAiProcessResult.builder()
                        .provider("groq")
                        .extractedText(oversizedContent)
                        .parts(List.of(LecturePartDraft.builder()
                                .partNumber(1).name("개념").content(oversizedContent).build()))
                        .build());

        service.process(new LectureUploadProcessingRequestedEvent(
                30L, 20L, 10L, 1L, "1장", StudyMaterialUploadType.TEXT,
                PartSplitMethod.AUTO, oversizedContent, List.of(), List.of()
        ));

        assertThat(upload.getStatus()).isEqualTo("failed");
        assertThat(processingJob.getFailCode()).isEqualTo(ErrorCode.LECTURE_CONTENT_ERROR.name());
        assertThat(processingJob.getFailReason()).isEqualTo("파일 내용이 너무 많음");
    }

    /**
     * Groq API Key 미설정 시 fail_code=CONFIG_ERROR 저장 검증
     */
    @Test
    void process_marks_failed_with_CONFIG_ERROR_when_groq_not_configured() {
        LectureUpload upload = LectureUpload.create(20L, 1L, StudyMaterialUploadType.TEXT, PartSplitMethod.AUTO);
        ReflectionTestUtils.setField(upload, "id", 30L);
        LectureProcessingJob processingJob = LectureProcessingJob.create(upload.getId(), 1L, 30);
        when(lectureUploadRepository.findById(30L)).thenReturn(Optional.of(upload));
        when(lectureProcessingJobRepository.findByLectureUploadId(upload.getId())).thenReturn(Optional.of(processingJob));
        when(lectureMaterialAiProcessor.process(any()))
                .thenThrow(new CustomException(ErrorCode.LECTURE_CONFIG_ERROR, "Groq 설정값이 준비되지 않았습니다."));

        service.process(new LectureUploadProcessingRequestedEvent(
                30L, 20L, 10L, 1L, "1장", StudyMaterialUploadType.TEXT,
                PartSplitMethod.AUTO, "원문 텍스트", List.of(), List.of()
        ));

        assertThat(upload.getStatus()).isEqualTo("failed");
        assertThat(processingJob.getFailCode()).isEqualTo(ErrorCode.LECTURE_CONFIG_ERROR.name());
    }

    /**
     * 명시되지 않은 예외 발생 시 fail_code=INFRA_ERROR 저장 검증 (fallback)
     */
    @Test
    void process_marks_failed_with_INFRA_ERROR_as_fallback_for_unknown_exception() {
        LectureUpload upload = LectureUpload.create(20L, 1L, StudyMaterialUploadType.TEXT, PartSplitMethod.AUTO);
        ReflectionTestUtils.setField(upload, "id", 30L);
        LectureProcessingJob processingJob = LectureProcessingJob.create(upload.getId(), 1L, 30);
        when(lectureUploadRepository.findById(30L)).thenReturn(Optional.of(upload));
        when(lectureProcessingJobRepository.findByLectureUploadId(upload.getId())).thenReturn(Optional.of(processingJob));
        when(lectureMaterialAiProcessor.process(any()))
                .thenThrow(new RuntimeException("unexpected error"));

        service.process(new LectureUploadProcessingRequestedEvent(
                30L, 20L, 10L, 1L, "1장", StudyMaterialUploadType.TEXT,
                PartSplitMethod.AUTO, "원문 텍스트", List.of(), List.of()
        ));

        assertThat(upload.getStatus()).isEqualTo("failed");
        assertThat(processingJob.getFailCode()).isEqualTo(ErrorCode.LECTURE_INFRA_ERROR.name());
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
