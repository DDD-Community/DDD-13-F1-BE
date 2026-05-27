package com.f1.quiket.domain.lecture.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.chapter.entity.Chapter;
import com.f1.quiket.domain.chapter.repository.ChapterRepository;
import com.f1.quiket.domain.lecture.dto.LectureTextUploadRequest;
import com.f1.quiket.domain.lecture.dto.LectureUploadAcceptedResponse;
import com.f1.quiket.domain.lecture.dto.PartSplitPlanRequest;
import com.f1.quiket.domain.lecture.entity.LectureUpload;
import com.f1.quiket.domain.lecture.event.LectureUploadProcessingRequestedEvent;
import com.f1.quiket.domain.lecture.repository.LectureUploadFileRepository;
import com.f1.quiket.domain.lecture.repository.LectureUploadRepository;
import com.f1.quiket.domain.lecture.repository.PartSplitPlanRepository;
import com.f1.quiket.domain.subject.entity.Subject;
import com.f1.quiket.domain.subject.repository.SubjectRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class LectureUploadCreateServiceTest {

    private SubjectRepository subjectRepository;
    private ChapterRepository chapterRepository;
    private LectureUploadRepository lectureUploadRepository;
    private LectureUploadFileRepository lectureUploadFileRepository;
    private PartSplitPlanRepository partSplitPlanRepository;
    private ApplicationEventPublisher eventPublisher;
    private LectureUploadCreateService service;

    @BeforeEach
    void setUp() {
        subjectRepository = mock(SubjectRepository.class);
        chapterRepository = mock(ChapterRepository.class);
        lectureUploadRepository = mock(LectureUploadRepository.class);
        lectureUploadFileRepository = mock(LectureUploadFileRepository.class);
        partSplitPlanRepository = mock(PartSplitPlanRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new LectureUploadCreateService(
                subjectRepository,
                chapterRepository,
                lectureUploadRepository,
                lectureUploadFileRepository,
                partSplitPlanRepository,
                eventPublisher
        );
    }

    @Test
    void create_text_upload_saves_chapter_upload_plan_and_publishes_event() {
        Subject subject = subject(10L, "subject-public-id", 1L);
        Chapter chapter = Chapter.create(subject.getId(), 1L, "1장", 1);
        ReflectionTestUtils.setField(chapter, "id", 20L);
        ReflectionTestUtils.setField(chapter, "publicId", "chapter-public-id");
        LectureUpload upload = LectureUpload.create(chapter.getId(), 1L,
                com.f1.quiket.domain.material.dto.StudyMaterialUploadType.TEXT,
                com.f1.quiket.domain.lecture.dto.PartSplitMethod.MANUAL);
        ReflectionTestUtils.setField(upload, "id", 30L);
        ReflectionTestUtils.setField(upload, "publicId", "upload-public-id");

        when(subjectRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(subject.getPublicId(), 1L))
                .thenReturn(Optional.of(subject));
        when(chapterRepository.findMaxDisplayOrderBySubjectId(subject.getId())).thenReturn(0);
        when(chapterRepository.save(any())).thenReturn(chapter);
        when(lectureUploadRepository.save(any())).thenReturn(upload);

        LectureUploadAcceptedResponse response = service.createTextUpload(1L, textRequest());

        assertThat(response.getLectureUploadId()).isEqualTo("upload-public-id");
        verify(partSplitPlanRepository).saveAll(any());
        ArgumentCaptor<LectureUploadProcessingRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(LectureUploadProcessingRequestedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().partSplitPlans()).hasSize(1);
        assertThat(eventCaptor.getValue().text()).hasSizeGreaterThanOrEqualTo(100);
    }

    @Test
    void create_text_upload_rejects_short_text() {
        LectureTextUploadRequest request = textRequest();
        ReflectionTestUtils.setField(request, "text", "짧은 텍스트");

        assertThatThrownBy(() -> service.createTextUpload(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void create_file_upload_rejects_non_pdf_file_when_upload_type_pdf() {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "lecture.txt",
                "text/plain",
                "content".getBytes()
        );

        assertThatThrownBy(() -> service.createFileUpload(
                1L,
                "subject-public-id",
                "1장",
                "pdf",
                "auto",
                List.of(file),
                null
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage("지원하지 않는 파일 형식이에요");
    }

    private LectureTextUploadRequest textRequest() {
        LectureTextUploadRequest request = new LectureTextUploadRequest();
        ReflectionTestUtils.setField(request, "subjectId", "subject-public-id");
        ReflectionTestUtils.setField(request, "chapterName", "1장");
        ReflectionTestUtils.setField(request, "uploadType", "text");
        ReflectionTestUtils.setField(request, "partSplitMethod", "manual");
        ReflectionTestUtils.setField(request, "text", "데이터 모델링은 현실 세계의 데이터를 추상화하여 구조화하는 과정입니다. ".repeat(3));
        PartSplitPlanRequest plan = new PartSplitPlanRequest();
        ReflectionTestUtils.setField(plan, "partNumber", 1);
        ReflectionTestUtils.setField(plan, "intendedName", "데이터 모델링 개념");
        ReflectionTestUtils.setField(request, "partSplitPlans", List.of(plan));
        return request;
    }

    private Subject subject(Long id, String publicId, Long userId) {
        Subject subject = Subject.create(userId, "데이터베이스", "exam");
        ReflectionTestUtils.setField(subject, "id", id);
        ReflectionTestUtils.setField(subject, "publicId", publicId);
        return subject;
    }
}
