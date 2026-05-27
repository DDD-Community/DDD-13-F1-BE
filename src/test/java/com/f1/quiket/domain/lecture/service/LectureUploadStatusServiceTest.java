package com.f1.quiket.domain.lecture.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.chapter.entity.Chapter;
import com.f1.quiket.domain.chapter.repository.ChapterRepository;
import com.f1.quiket.domain.lecture.dto.LectureUploadStatusResponse;
import com.f1.quiket.domain.lecture.dto.PartSplitMethod;
import com.f1.quiket.domain.lecture.entity.LectureUpload;
import com.f1.quiket.domain.lecture.repository.LectureUploadRepository;
import com.f1.quiket.domain.material.dto.StudyMaterialUploadType;
import com.f1.quiket.domain.part.entity.Part;
import com.f1.quiket.domain.part.repository.PartRepository;
import com.f1.quiket.domain.subject.entity.Subject;
import com.f1.quiket.domain.subject.repository.SubjectRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class LectureUploadStatusServiceTest {

    private LectureUploadRepository lectureUploadRepository;
    private ChapterRepository chapterRepository;
    private SubjectRepository subjectRepository;
    private PartRepository partRepository;
    private LectureUploadStatusService service;

    @BeforeEach
    void setUp() {
        lectureUploadRepository = mock(LectureUploadRepository.class);
        chapterRepository = mock(ChapterRepository.class);
        subjectRepository = mock(SubjectRepository.class);
        partRepository = mock(PartRepository.class);
        service = new LectureUploadStatusService(
                lectureUploadRepository,
                chapterRepository,
                subjectRepository,
                partRepository
        );
    }

    @Test
    void getStatus_returns_completed_status_with_parts() {
        Subject subject = Subject.create(1L, "데이터베이스", "exam");
        ReflectionTestUtils.setField(subject, "id", 10L);
        ReflectionTestUtils.setField(subject, "publicId", "subject-public-id");
        Chapter chapter = Chapter.create(subject.getId(), 1L, "1장", 1);
        ReflectionTestUtils.setField(chapter, "id", 20L);
        ReflectionTestUtils.setField(chapter, "publicId", "chapter-public-id");
        LectureUpload upload = LectureUpload.create(chapter.getId(), 1L, StudyMaterialUploadType.TEXT, PartSplitMethod.AUTO);
        ReflectionTestUtils.setField(upload, "id", 30L);
        ReflectionTestUtils.setField(upload, "publicId", "upload-public-id");
        upload.markCompleted("원문 텍스트");
        Part part = Part.createFromLectureUpload(chapter.getId(), subject.getId(), 1L, upload.getId(), "개념", 1, "원문 텍스트");
        ReflectionTestUtils.setField(part, "publicId", "part-public-id");

        when(lectureUploadRepository.findByPublicIdAndUserIdAndDeletedAtIsNull("upload-public-id", 1L))
                .thenReturn(Optional.of(upload));
        when(chapterRepository.findById(chapter.getId())).thenReturn(Optional.of(chapter));
        when(subjectRepository.findByIdAndUserIdAndDeletedAtIsNull(subject.getId(), 1L)).thenReturn(Optional.of(subject));
        when(partRepository.findAllByLectureUploadIdAndUserIdAndDeletedAtIsNullOrderByPartNumberAsc(upload.getId(), 1L))
                .thenReturn(List.of(part));

        LectureUploadStatusResponse response = service.getStatus(1L, "upload-public-id");

        assertThat(response.getStatus()).isEqualTo("completed");
        assertThat(response.getProgressPct()).isEqualTo(100);
        assertThat(response.getParts()).hasSize(1);
        assertThat(response.getParts().get(0).getId()).isEqualTo("part-public-id");
    }
}
