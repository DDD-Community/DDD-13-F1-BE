package com.f1.quiket.domain.part.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.chapter.entity.Chapter;
import com.f1.quiket.domain.chapter.repository.ChapterRepository;
import com.f1.quiket.domain.lecture.dto.LectureUploadAcceptedResponse;
import com.f1.quiket.domain.lecture.entity.LectureUpload;
import com.f1.quiket.domain.lecture.repository.LectureProcessingJobRepository;
import com.f1.quiket.domain.lecture.repository.LectureUploadFileRepository;
import com.f1.quiket.domain.lecture.repository.LectureUploadRepository;
import com.f1.quiket.domain.part.dto.PartTextAddRequest;
import com.f1.quiket.domain.part.event.PartAddProcessingRequestedEvent;
import com.f1.quiket.domain.part.repository.PartRepository;
import com.f1.quiket.domain.subject.entity.Subject;
import com.f1.quiket.domain.subject.repository.SubjectRepository;
import com.f1.quiket.global.error.CustomException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class PartAddCreateServiceTest {

    private ChapterRepository chapterRepository;
    private SubjectRepository subjectRepository;
    private PartRepository partRepository;
    private LectureUploadRepository lectureUploadRepository;
    private LectureUploadFileRepository lectureUploadFileRepository;
    private LectureProcessingJobRepository lectureProcessingJobRepository;
    private ApplicationEventPublisher eventPublisher;
    private PartAddCreateService service;

    @BeforeEach
    void setUp() {
        chapterRepository = mock(ChapterRepository.class);
        subjectRepository = mock(SubjectRepository.class);
        partRepository = mock(PartRepository.class);
        lectureUploadRepository = mock(LectureUploadRepository.class);
        lectureUploadFileRepository = mock(LectureUploadFileRepository.class);
        lectureProcessingJobRepository = mock(LectureProcessingJobRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new PartAddCreateService(
                chapterRepository,
                subjectRepository,
                partRepository,
                lectureUploadRepository,
                lectureUploadFileRepository,
                lectureProcessingJobRepository,
                eventPublisher
        );
    }

    @Test
    void create_text_part_saves_upload_and_publishes_single_part_event() {
        Subject subject = subject(10L, "subject-public-id", 1L);
        Chapter chapter = chapter(20L, "chapter-public-id", subject.getId(), 1L);
        LectureUpload upload = LectureUpload.create(chapter.getId(), 1L,
                com.f1.quiket.domain.material.dto.StudyMaterialUploadType.TEXT,
                com.f1.quiket.domain.lecture.dto.PartSplitMethod.MANUAL);
        ReflectionTestUtils.setField(upload, "id", 30L);
        ReflectionTestUtils.setField(upload, "publicId", "upload-public-id");

        when(chapterRepository.findForUpdateByPublicIdAndUserIdAndDeletedAtIsNull(chapter.getPublicId(), 1L))
                .thenReturn(Optional.of(chapter));
        when(subjectRepository.findByIdAndUserIdAndDeletedAtIsNull(subject.getId(), 1L)).thenReturn(Optional.of(subject));
        when(partRepository.findMaxPartNumberByChapterId(chapter.getId())).thenReturn(2);
        when(lectureUploadRepository.save(any())).thenReturn(upload);

        LectureUploadAcceptedResponse response = service.createTextPart(1L, chapter.getPublicId(), textRequest());

        assertThat(response.getLectureUploadId()).isEqualTo("upload-public-id");
        verify(lectureProcessingJobRepository).save(any());
        ArgumentCaptor<PartAddProcessingRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(PartAddProcessingRequestedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().partNumber()).isEqualTo(3);
        assertThat(eventCaptor.getValue().partName()).isEqualTo("새 파트");
        assertThat(eventCaptor.getValue().text()).hasSizeGreaterThanOrEqualTo(100);
        assertThat(upload.getRawText()).isEqualTo(eventCaptor.getValue().text());
    }

    @Test
    void create_text_part_rejects_short_text() {
        PartTextAddRequest request = textRequest();
        ReflectionTestUtils.setField(request, "text", "짧은 텍스트");

        assertThatThrownBy(() -> service.createTextPart(1L, "chapter-public-id", request))
                .isInstanceOf(CustomException.class)
                .hasMessage("text는 100자 이상 입력해주세요.");
    }

    @Test
    void create_file_part_rejects_non_pdf_file_when_upload_type_pdf() {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "lecture.txt",
                "text/plain",
                "content".getBytes()
        );

        assertThatThrownBy(() -> service.createFilePart(
                1L,
                "chapter-public-id",
                "새 파트",
                "pdf",
                List.of(file)
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage("지원하지 않는 파일 형식이에요");
    }

    private PartTextAddRequest textRequest() {
        PartTextAddRequest request = new PartTextAddRequest();
        ReflectionTestUtils.setField(request, "partName", "새 파트");
        ReflectionTestUtils.setField(request, "uploadType", "text");
        ReflectionTestUtils.setField(request, "text", "데이터 모델링은 현실 세계의 데이터를 추상화하여 구조화하는 과정입니다. ".repeat(3));
        return request;
    }

    private Subject subject(Long id, String publicId, Long userId) {
        Subject subject = Subject.create(userId, "데이터베이스", "exam");
        ReflectionTestUtils.setField(subject, "id", id);
        ReflectionTestUtils.setField(subject, "publicId", publicId);
        return subject;
    }

    private Chapter chapter(Long id, String publicId, Long subjectId, Long userId) {
        Chapter chapter = Chapter.create(subjectId, userId, "1장", 1);
        ReflectionTestUtils.setField(chapter, "id", id);
        ReflectionTestUtils.setField(chapter, "publicId", publicId);
        return chapter;
    }
}
