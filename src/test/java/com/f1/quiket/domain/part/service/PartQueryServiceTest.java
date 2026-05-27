package com.f1.quiket.domain.part.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.chapter.entity.Chapter;
import com.f1.quiket.domain.chapter.repository.ChapterRepository;
import com.f1.quiket.domain.lecture.entity.LectureUpload;
import com.f1.quiket.domain.lecture.repository.LectureUploadRepository;
import com.f1.quiket.domain.material.dto.StudyMaterialUploadType;
import com.f1.quiket.domain.lecture.dto.PartSplitMethod;
import com.f1.quiket.domain.part.dto.PartResponse;
import com.f1.quiket.domain.part.dto.PartUpdateRequest;
import com.f1.quiket.domain.part.entity.Part;
import com.f1.quiket.domain.part.repository.PartRepository;
import com.f1.quiket.domain.subject.entity.Subject;
import com.f1.quiket.domain.subject.repository.SubjectRepository;
import com.f1.quiket.global.error.CustomException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PartQueryServiceTest {

    private PartRepository partRepository;
    private SubjectRepository subjectRepository;
    private ChapterRepository chapterRepository;
    private LectureUploadRepository lectureUploadRepository;
    private PartQueryService service;

    @BeforeEach
    void setUp() {
        partRepository = mock(PartRepository.class);
        subjectRepository = mock(SubjectRepository.class);
        chapterRepository = mock(ChapterRepository.class);
        lectureUploadRepository = mock(LectureUploadRepository.class);
        service = new PartQueryService(partRepository, subjectRepository, chapterRepository, lectureUploadRepository);
    }

    @Test
    void get_part_returns_public_ids_and_content_preview() {
        Subject subject = subject(10L, "subject-public-id", 1L);
        Chapter chapter = chapter(20L, "chapter-public-id", subject.getId(), 1L);
        LectureUpload upload = lectureUpload(30L, "upload-public-id", chapter.getId(), 1L);
        Part part = part(40L, "part-public-id", chapter, subject, upload);

        when(partRepository.findByPublicIdAndUserIdAndContentDeletedFalseAndDeletedAtIsNull(part.getPublicId(), 1L))
                .thenReturn(Optional.of(part));
        when(subjectRepository.findByIdAndUserIdAndDeletedAtIsNull(subject.getId(), 1L)).thenReturn(Optional.of(subject));
        when(chapterRepository.findById(chapter.getId())).thenReturn(Optional.of(chapter));
        when(lectureUploadRepository.findById(upload.getId())).thenReturn(Optional.of(upload));

        PartResponse response = service.getPart(1L, part.getPublicId());

        assertThat(response.getId()).isEqualTo("part-public-id");
        assertThat(response.getSubjectId()).isEqualTo("subject-public-id");
        assertThat(response.getChapterId()).isEqualTo("chapter-public-id");
        assertThat(response.getLectureUploadId()).isEqualTo("upload-public-id");
        assertThat(response.getContentPreview()).hasSize(30);
    }

    @Test
    void update_part_trims_name_and_content_without_changing_part_number() {
        Subject subject = subject(10L, "subject-public-id", 1L);
        Chapter chapter = chapter(20L, "chapter-public-id", subject.getId(), 1L);
        LectureUpload upload = lectureUpload(30L, "upload-public-id", chapter.getId(), 1L);
        Part part = part(40L, "part-public-id", chapter, subject, upload);
        PartUpdateRequest request = new PartUpdateRequest();
        ReflectionTestUtils.setField(request, "name", " 수정 파트 ");
        ReflectionTestUtils.setField(request, "content", " 수정 본문 ");

        when(partRepository.findByPublicIdAndUserIdAndContentDeletedFalseAndDeletedAtIsNull(part.getPublicId(), 1L))
                .thenReturn(Optional.of(part));
        when(subjectRepository.findByIdAndUserIdAndDeletedAtIsNull(subject.getId(), 1L)).thenReturn(Optional.of(subject));
        when(chapterRepository.findById(chapter.getId())).thenReturn(Optional.of(chapter));
        when(lectureUploadRepository.findById(upload.getId())).thenReturn(Optional.of(upload));

        PartResponse response = service.updatePart(1L, part.getPublicId(), request);

        assertThat(response.getName()).isEqualTo("수정 파트");
        assertThat(response.getContent()).isEqualTo("수정 본문");
        assertThat(response.getPartNumber()).isEqualTo(3);
        assertThat(part.getPartNumber()).isEqualTo(3);
    }

    @Test
    void update_part_rejects_blank_content() {
        PartUpdateRequest request = new PartUpdateRequest();
        ReflectionTestUtils.setField(request, "name", "파트");
        ReflectionTestUtils.setField(request, "content", " ");
        Part part = Part.createFromLectureUpload(20L, 10L, 1L, null, "파트", 1, "본문");
        ReflectionTestUtils.setField(part, "publicId", "part-public-id");

        when(partRepository.findByPublicIdAndUserIdAndContentDeletedFalseAndDeletedAtIsNull(part.getPublicId(), 1L))
                .thenReturn(Optional.of(part));

        assertThatThrownBy(() -> service.updatePart(1L, part.getPublicId(), request))
                .isInstanceOf(CustomException.class)
                .hasMessage("파트 내용을 입력해주세요");
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

    private LectureUpload lectureUpload(Long id, String publicId, Long chapterId, Long userId) {
        LectureUpload upload = LectureUpload.create(chapterId, userId, StudyMaterialUploadType.TEXT, PartSplitMethod.MANUAL);
        ReflectionTestUtils.setField(upload, "id", id);
        ReflectionTestUtils.setField(upload, "publicId", publicId);
        return upload;
    }

    private Part part(Long id, String publicId, Chapter chapter, Subject subject, LectureUpload upload) {
        Part part = Part.createFromLectureUpload(
                chapter.getId(),
                subject.getId(),
                subject.getUserId(),
                upload.getId(),
                "데이터 모델링 개념",
                3,
                "데이터 모델링은 현실 세계의 데이터를 추상화하여 구조화하는 과정입니다."
        );
        ReflectionTestUtils.setField(part, "id", id);
        ReflectionTestUtils.setField(part, "publicId", publicId);
        return part;
    }
}
