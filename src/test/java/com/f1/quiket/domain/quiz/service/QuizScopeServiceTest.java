package com.f1.quiket.domain.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.chapter.entity.Chapter;
import com.f1.quiket.domain.chapter.repository.ChapterRepository;
import com.f1.quiket.domain.part.entity.Part;
import com.f1.quiket.domain.part.repository.PartRepository;
import com.f1.quiket.domain.quiz.dto.QuizScopeResponse;
import com.f1.quiket.domain.subject.entity.Subject;
import com.f1.quiket.domain.subject.repository.SubjectRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class QuizScopeServiceTest {

    private SubjectRepository subjectRepository;
    private ChapterRepository chapterRepository;
    private PartRepository partRepository;
    private QuizScopeService quizScopeService;

    @BeforeEach
    void setUp() {
        subjectRepository = mock(SubjectRepository.class);
        chapterRepository = mock(ChapterRepository.class);
        partRepository = mock(PartRepository.class);
        quizScopeService = new QuizScopeService(subjectRepository, chapterRepository, partRepository);
    }

    @Test
    void getQuizScope_returns_chapters_with_parts() {
        Long userId = 1L;
        Subject subject = subject(10L, "subject-public-id", userId, "데이터베이스");
        Chapter chapter1 = chapter(100L, "chapter-public-1", subject.getId(), userId, "1장 데이터 모델링", 1);
        Chapter chapter2 = chapter(101L, "chapter-public-2", subject.getId(), userId, "2장 정규화", 2);
        Part part1 = part(1000L, "part-public-1", chapter1.getId(), subject.getId(), userId, "데이터 모델링 개념", 1, "데이터 모델링은 현실 세계의 데이터를 추상화하여 구조화하는 과정입니다.");
        Part part2 = part(1001L, "part-public-2", chapter1.getId(), subject.getId(), userId, "데이터 모델링 특징", 2, null);

        when(subjectRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(subject.getPublicId(), userId))
                .thenReturn(Optional.of(subject));
        when(chapterRepository.findAllBySubjectIdAndUserIdAndDeletedAtIsNullOrderByDisplayOrderAscCreatedAtAsc(subject.getId(), userId))
                .thenReturn(List.of(chapter1, chapter2));
        when(partRepository.findAllBySubjectIdAndUserIdAndDeletedAtIsNullOrderByChapterIdAscPartNumberAscCreatedAtAsc(subject.getId(), userId))
                .thenReturn(List.of(part1, part2));

        QuizScopeResponse response = quizScopeService.getQuizScope(userId, subject.getPublicId());

        assertThat(response.getSubjectId()).isEqualTo(subject.getPublicId());
        assertThat(response.getSubjectName()).isEqualTo("데이터베이스");
        assertThat(response.getChapters()).hasSize(2);
        assertThat(response.getChapters().get(0).getId()).isEqualTo("chapter-public-1");
        assertThat(response.getChapters().get(0).getParts()).hasSize(2);
        assertThat(response.getChapters().get(0).getParts().get(0).getId()).isEqualTo("part-public-1");
        assertThat(response.getChapters().get(0).getParts().get(0).getChapterId()).isEqualTo("chapter-public-1");
        assertThat(response.getChapters().get(0).getParts().get(0).getContentPreview()).endsWith("...");
        assertThat(response.getChapters().get(0).getParts().get(1).getContentPreview()).isNull();
        assertThat(response.getChapters().get(1).getParts()).isEmpty();
    }

    @Test
    void getQuizScope_throws_not_found_when_subject_is_not_owned() {
        Long userId = 1L;
        String subjectPublicId = "subject-public-id";
        when(subjectRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(subjectPublicId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> quizScopeService.getQuizScope(userId, subjectPublicId))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.SUBJECT_NOT_FOUND);

        verifyNoInteractions(chapterRepository, partRepository);
    }

    @Test
    void getQuizScope_returns_empty_chapters_when_subject_has_none() {
        Long userId = 1L;
        Subject subject = subject(10L, "subject-public-id", userId, "데이터베이스");

        when(subjectRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(subject.getPublicId(), userId))
                .thenReturn(Optional.of(subject));
        when(chapterRepository.findAllBySubjectIdAndUserIdAndDeletedAtIsNullOrderByDisplayOrderAscCreatedAtAsc(subject.getId(), userId))
                .thenReturn(List.of());
        when(partRepository.findAllBySubjectIdAndUserIdAndDeletedAtIsNullOrderByChapterIdAscPartNumberAscCreatedAtAsc(subject.getId(), userId))
                .thenReturn(List.of());

        QuizScopeResponse response = quizScopeService.getQuizScope(userId, subject.getPublicId());

        assertThat(response.getSubjectId()).isEqualTo(subject.getPublicId());
        assertThat(response.getSubjectName()).isEqualTo("데이터베이스");
        assertThat(response.getChapters()).isEmpty();
    }

    private Subject subject(Long id, String publicId, Long userId, String name) {
        Subject subject = newEntity(Subject.class);
        ReflectionTestUtils.setField(subject, "id", id);
        ReflectionTestUtils.setField(subject, "publicId", publicId);
        ReflectionTestUtils.setField(subject, "userId", userId);
        ReflectionTestUtils.setField(subject, "name", name);
        ReflectionTestUtils.setField(subject, "purpose", "exam");
        return subject;
    }

    private Chapter chapter(Long id, String publicId, Long subjectId, Long userId, String name, Integer displayOrder) {
        Chapter chapter = newEntity(Chapter.class);
        ReflectionTestUtils.setField(chapter, "id", id);
        ReflectionTestUtils.setField(chapter, "publicId", publicId);
        ReflectionTestUtils.setField(chapter, "subjectId", subjectId);
        ReflectionTestUtils.setField(chapter, "userId", userId);
        ReflectionTestUtils.setField(chapter, "name", name);
        ReflectionTestUtils.setField(chapter, "displayOrder", displayOrder);
        return chapter;
    }

    private Part part(Long id, String publicId, Long chapterId, Long subjectId, Long userId, String name, Integer partNumber, String content) {
        Part part = newEntity(Part.class);
        ReflectionTestUtils.setField(part, "id", id);
        ReflectionTestUtils.setField(part, "publicId", publicId);
        ReflectionTestUtils.setField(part, "chapterId", chapterId);
        ReflectionTestUtils.setField(part, "subjectId", subjectId);
        ReflectionTestUtils.setField(part, "userId", userId);
        ReflectionTestUtils.setField(part, "name", name);
        ReflectionTestUtils.setField(part, "partNumber", partNumber);
        ReflectionTestUtils.setField(part, "content", content);
        return part;
    }

    private <T> T newEntity(Class<T> type) {
        return org.springframework.beans.BeanUtils.instantiateClass(type);
    }
}
