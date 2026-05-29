package com.f1.quiket.domain.subject.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.chapter.entity.Chapter;
import com.f1.quiket.domain.chapter.repository.ChapterRepository;
import com.f1.quiket.domain.part.entity.Part;
import com.f1.quiket.domain.part.repository.PartRepository;
import com.f1.quiket.domain.quiz.entity.QuizSession;
import com.f1.quiket.domain.quiz.repository.QuizSessionRepository;
import com.f1.quiket.domain.subject.dto.SubjectExamScheduleResponse;
import com.f1.quiket.domain.subject.dto.SubjectExamScheduleUpsertRequest;
import com.f1.quiket.domain.subject.dto.SubjectPageResponse;
import com.f1.quiket.domain.subject.entity.Subject;
import com.f1.quiket.domain.subject.entity.SubjectExamSchedule;
import com.f1.quiket.domain.subject.repository.SubjectExamDetailRepository;
import com.f1.quiket.domain.subject.repository.SubjectExamScheduleRepository;
import com.f1.quiket.domain.subject.repository.SubjectOtherDetailRepository;
import com.f1.quiket.domain.subject.repository.SubjectRepository;
import com.f1.quiket.domain.subject.repository.SubjectReviewDetailRepository;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.domain.user.repository.UserRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

class SubjectServiceTest {

    private UserRepository userRepository;
    private SubjectRepository subjectRepository;
    private SubjectExamDetailRepository subjectExamDetailRepository;
    private SubjectReviewDetailRepository subjectReviewDetailRepository;
    private SubjectOtherDetailRepository subjectOtherDetailRepository;
    private SubjectExamScheduleRepository subjectExamScheduleRepository;
    private ChapterRepository chapterRepository;
    private PartRepository partRepository;
    private QuizSessionRepository quizSessionRepository;
    private SubjectService subjectService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        subjectRepository = mock(SubjectRepository.class);
        subjectExamDetailRepository = mock(SubjectExamDetailRepository.class);
        subjectReviewDetailRepository = mock(SubjectReviewDetailRepository.class);
        subjectOtherDetailRepository = mock(SubjectOtherDetailRepository.class);
        subjectExamScheduleRepository = mock(SubjectExamScheduleRepository.class);
        chapterRepository = mock(ChapterRepository.class);
        partRepository = mock(PartRepository.class);
        quizSessionRepository = mock(QuizSessionRepository.class);

        subjectService = new SubjectService(
                userRepository,
                subjectRepository,
                subjectExamDetailRepository,
                subjectReviewDetailRepository,
                subjectOtherDetailRepository,
                subjectExamScheduleRepository,
                chapterRepository,
                partRepository,
                quizSessionRepository
        );
    }

    @Test
    void getSubjects_returns_empty_page_with_normalized_page_and_size() {
        User user = user(1L, "user-public-id");
        when(userRepository.findByPublicIdAndDeletedAtIsNull(user.getPublicId())).thenReturn(Optional.of(user));

        Page<Subject> page = new PageImpl<>(List.of(), org.springframework.data.domain.PageRequest.of(0, 100), 0);
        when(subjectRepository.findByUserIdAndDeletedAtIsNull(eq(user.getId()), any(Pageable.class))).thenReturn(page);

        SubjectPageResponse response = subjectService.getSubjects(user.getPublicId(), -1, 999);

        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(100);
        assertThat(response.getContent()).isEmpty();
        verify(subjectRepository).findByUserIdAndDeletedAtIsNull(eq(user.getId()), any(Pageable.class));
        verifyNoInteractions(chapterRepository, partRepository, subjectExamScheduleRepository);
    }

    @Test
    void upsertExamSchedule_creates_new_schedule_with_normalized_exam_name() {
        User user = user(1L, "user-public-id");
        Subject subject = subject(10L, "subject-public-id", user.getId(), "데이터베이스");
        SubjectExamScheduleUpsertRequest request = new SubjectExamScheduleUpsertRequest();
        ReflectionTestUtils.setField(request, "examName", " ");
        ReflectionTestUtils.setField(request, "examDate", LocalDate.now().plusDays(7));

        when(userRepository.findByPublicIdAndDeletedAtIsNull(user.getPublicId())).thenReturn(Optional.of(user));
        when(subjectRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(subject.getPublicId(), user.getId())).thenReturn(Optional.of(subject));
        when(subjectExamScheduleRepository.findBySubjectIdIncludingDeleted(subject.getId())).thenReturn(Optional.empty());
        when(subjectExamScheduleRepository.save(any(SubjectExamSchedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SubjectExamScheduleResponse response = subjectService.upsertExamSchedule(user.getPublicId(), subject.getPublicId(), request);

        ArgumentCaptor<SubjectExamSchedule> captor = ArgumentCaptor.forClass(SubjectExamSchedule.class);
        verify(subjectExamScheduleRepository).save(captor.capture());
        assertThat(captor.getValue().getExamName()).isNull();
        assertThat(captor.getValue().getExamDate()).isEqualTo(request.getExamDate());

        assertThat(response.getSubjectId()).isEqualTo(subject.getPublicId());
        assertThat(response.getExamName()).isEqualTo(subject.getName());
        assertThat(response.getExamDate()).isEqualTo(request.getExamDate());
    }

    @Test
    void upsertExamSchedule_restores_deleted_schedule_without_new_insert() {
        User user = user(1L, "user-public-id");
        Subject subject = subject(10L, "subject-public-id", user.getId(), "데이터베이스");
        SubjectExamSchedule schedule = schedule(100L, "schedule-public-id", subject.getId(), user.getId(), "중간고사", LocalDate.now().plusDays(1));
        schedule.delete();

        SubjectExamScheduleUpsertRequest request = new SubjectExamScheduleUpsertRequest();
        ReflectionTestUtils.setField(request, "examName", "기말고사");
        ReflectionTestUtils.setField(request, "examDate", LocalDate.now().plusDays(7));

        when(userRepository.findByPublicIdAndDeletedAtIsNull(user.getPublicId())).thenReturn(Optional.of(user));
        when(subjectRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(subject.getPublicId(), user.getId())).thenReturn(Optional.of(subject));
        when(subjectExamScheduleRepository.findBySubjectIdIncludingDeleted(subject.getId())).thenReturn(Optional.of(schedule));

        SubjectExamScheduleResponse response = subjectService.upsertExamSchedule(user.getPublicId(), subject.getPublicId(), request);

        verify(subjectExamScheduleRepository, never()).save(any(SubjectExamSchedule.class));
        assertThat(schedule.getDeletedAt()).isNull();
        assertThat(schedule.getExamName()).isEqualTo("기말고사");
        assertThat(schedule.getExamDate()).isEqualTo(request.getExamDate());
        assertThat(response.getId()).isEqualTo(schedule.getPublicId());
    }

    @Test
    void deleteExamSchedule_throws_not_found_when_schedule_missing() {
        User user = user(1L, "user-public-id");
        Subject subject = subject(10L, "subject-public-id", user.getId(), "데이터베이스");
        when(userRepository.findByPublicIdAndDeletedAtIsNull(user.getPublicId())).thenReturn(Optional.of(user));
        when(subjectRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(subject.getPublicId(), user.getId())).thenReturn(Optional.of(subject));
        when(subjectExamScheduleRepository.findBySubjectIdAndDeletedAtIsNull(subject.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.deleteExamSchedule(user.getPublicId(), subject.getPublicId()))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void deleteSubject_soft_deletes_subject_and_children() {
        User user = user(1L, "user-public-id");
        Subject subject = subject(10L, "subject-public-id", user.getId(), "데이터베이스");
        SubjectExamSchedule schedule = schedule(100L, "schedule-public-id", subject.getId(), user.getId(), "기말고사", LocalDate.now().plusDays(2));
        Chapter chapter = chapter(200L, "chapter-public-id", subject.getId(), user.getId(), "트랜잭션", 1);
        Part part = part(300L, "part-public-id", chapter.getId(), subject.getId(), user.getId(), "락", 1);
        QuizSession quizSession = quizSession(400L, "quiz-public-id", user.getId(), subject.getId());

        when(userRepository.findByPublicIdAndDeletedAtIsNull(user.getPublicId())).thenReturn(Optional.of(user));
        when(subjectRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(subject.getPublicId(), user.getId())).thenReturn(Optional.of(subject));
        when(subjectExamScheduleRepository.findBySubjectIdAndDeletedAtIsNull(subject.getId())).thenReturn(Optional.of(schedule));
        when(chapterRepository.findAllBySubjectIdAndUserIdAndDeletedAtIsNullOrderByDisplayOrderAscCreatedAtAsc(subject.getId(), user.getId()))
                .thenReturn(List.of(chapter));
        when(partRepository.findAllBySubjectIdAndDeletedAtIsNull(subject.getId())).thenReturn(List.of(part));
        when(quizSessionRepository.findAllBySubjectIdAndDeletedAtIsNull(subject.getId())).thenReturn(List.of(quizSession));

        subjectService.deleteSubject(user.getPublicId(), subject.getPublicId());

        assertThat(schedule.getDeletedAt()).isNotNull();
        assertThat(chapter.getDeletedAt()).isNotNull();
        assertThat(part.getDeletedAt()).isNotNull();
        assertThat(quizSession.getDeletedAt()).isNotNull();
        assertThat(subject.getDeletedAt()).isNotNull();
    }

    private User user(Long id, String publicId) {
        User user = User.create(publicId, "user@example.com", "도토리");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
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

    private SubjectExamSchedule schedule(Long id, String publicId, Long subjectId, Long userId, String examName, LocalDate examDate) {
        SubjectExamSchedule schedule = newEntity(SubjectExamSchedule.class);
        ReflectionTestUtils.setField(schedule, "id", id);
        ReflectionTestUtils.setField(schedule, "publicId", publicId);
        ReflectionTestUtils.setField(schedule, "subjectId", subjectId);
        ReflectionTestUtils.setField(schedule, "userId", userId);
        ReflectionTestUtils.setField(schedule, "examName", examName);
        ReflectionTestUtils.setField(schedule, "examDate", examDate);
        return schedule;
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

    private Part part(Long id, String publicId, Long chapterId, Long subjectId, Long userId, String name, Integer partNumber) {
        Part part = newEntity(Part.class);
        ReflectionTestUtils.setField(part, "id", id);
        ReflectionTestUtils.setField(part, "publicId", publicId);
        ReflectionTestUtils.setField(part, "chapterId", chapterId);
        ReflectionTestUtils.setField(part, "subjectId", subjectId);
        ReflectionTestUtils.setField(part, "userId", userId);
        ReflectionTestUtils.setField(part, "name", name);
        ReflectionTestUtils.setField(part, "partNumber", partNumber);
        return part;
    }

    private QuizSession quizSession(Long id, String publicId, Long userId, Long subjectId) {
        QuizSession quizSession = newEntity(QuizSession.class);
        ReflectionTestUtils.setField(quizSession, "id", id);
        ReflectionTestUtils.setField(quizSession, "publicId", publicId);
        ReflectionTestUtils.setField(quizSession, "userId", userId);
        ReflectionTestUtils.setField(quizSession, "subjectId", subjectId);
        ReflectionTestUtils.setField(quizSession, "status", "completed");
        return quizSession;
    }

    private <T> T newEntity(Class<T> type) {
        return org.springframework.beans.BeanUtils.instantiateClass(type);
    }
}
