package com.f1.quiket.domain.chapter.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.chapter.dto.ChapterResponse;
import com.f1.quiket.domain.chapter.entity.Chapter;
import com.f1.quiket.domain.chapter.repository.ChapterRepository;
import com.f1.quiket.domain.subject.entity.Subject;
import com.f1.quiket.domain.subject.repository.SubjectRepository;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.domain.user.repository.UserRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ChapterServiceTest {

    private UserRepository userRepository;
    private ChapterRepository chapterRepository;
    private SubjectRepository subjectRepository;
    private ChapterService chapterService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        chapterRepository = mock(ChapterRepository.class);
        subjectRepository = mock(SubjectRepository.class);
        chapterService = new ChapterService(userRepository, chapterRepository, subjectRepository);
    }

    @Test
    void updateChapterName_returns_updated_response() {
        User user = user(1L, "user-public-id");
        Chapter chapter = chapter(10L, "chapter-public-id", 100L, 1L, "기존 챕터", 1);
        Subject subject = subject(100L, "subject-public-id", 1L, "데이터베이스");

        when(userRepository.findByPublicIdAndDeletedAtIsNull(user.getPublicId())).thenReturn(Optional.of(user));
        when(chapterRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(chapter.getPublicId(), user.getId())).thenReturn(Optional.of(chapter));
        when(subjectRepository.findById(chapter.getSubjectId())).thenReturn(Optional.of(subject));

        ChapterResponse response = chapterService.updateChapterName(user.getPublicId(), chapter.getPublicId(), "트랜잭션");

        assertThat(response.getId()).isEqualTo(chapter.getPublicId());
        assertThat(response.getSubjectId()).isEqualTo(subject.getPublicId());
        assertThat(response.getName()).isEqualTo("트랜잭션");
        assertThat(chapter.getName()).isEqualTo("트랜잭션");
    }

    @Test
    void updateChapterName_throws_auth_user_not_found_when_user_missing() {
        when(userRepository.findByPublicIdAndDeletedAtIsNull("missing-user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chapterService.updateChapterName("missing-user", "chapter-public-id", "트랜잭션"))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_USER_NOT_FOUND);
    }

    @Test
    void updateChapterName_throws_not_found_when_chapter_missing() {
        User user = user(1L, "user-public-id");
        when(userRepository.findByPublicIdAndDeletedAtIsNull(user.getPublicId())).thenReturn(Optional.of(user));
        when(chapterRepository.findByPublicIdAndUserIdAndDeletedAtIsNull("missing-chapter", user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chapterService.updateChapterName(user.getPublicId(), "missing-chapter", "트랜잭션"))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void updateChapterName_throws_not_found_when_subject_missing() {
        User user = user(1L, "user-public-id");
        Chapter chapter = chapter(10L, "chapter-public-id", 100L, 1L, "기존 챕터", 1);
        when(userRepository.findByPublicIdAndDeletedAtIsNull(user.getPublicId())).thenReturn(Optional.of(user));
        when(chapterRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(chapter.getPublicId(), user.getId())).thenReturn(Optional.of(chapter));
        when(subjectRepository.findById(chapter.getSubjectId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chapterService.updateChapterName(user.getPublicId(), chapter.getPublicId(), "트랜잭션"))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    private User user(Long id, String publicId) {
        User user = User.create(publicId, "user@example.com", "도토리");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
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

    private Subject subject(Long id, String publicId, Long userId, String name) {
        Subject subject = newEntity(Subject.class);
        ReflectionTestUtils.setField(subject, "id", id);
        ReflectionTestUtils.setField(subject, "publicId", publicId);
        ReflectionTestUtils.setField(subject, "userId", userId);
        ReflectionTestUtils.setField(subject, "name", name);
        ReflectionTestUtils.setField(subject, "purpose", "exam");
        return subject;
    }

    private <T> T newEntity(Class<T> type) {
        return org.springframework.beans.BeanUtils.instantiateClass(type);
    }
}
