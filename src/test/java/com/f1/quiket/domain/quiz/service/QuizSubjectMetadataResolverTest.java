package com.f1.quiket.domain.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.subject.entity.Certificate;
import com.f1.quiket.domain.subject.entity.Subject;
import com.f1.quiket.domain.subject.entity.SubjectExamDetail;
import com.f1.quiket.domain.subject.entity.SubjectOtherDetail;
import com.f1.quiket.domain.subject.entity.SubjectReviewDetail;
import com.f1.quiket.domain.subject.repository.CertificateRepository;
import com.f1.quiket.domain.subject.repository.SubjectExamDetailRepository;
import com.f1.quiket.domain.subject.repository.SubjectOtherDetailRepository;
import com.f1.quiket.domain.subject.repository.SubjectReviewDetailRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

class QuizSubjectMetadataResolverTest {

    private SubjectExamDetailRepository subjectExamDetailRepository;
    private SubjectReviewDetailRepository subjectReviewDetailRepository;
    private SubjectOtherDetailRepository subjectOtherDetailRepository;
    private CertificateRepository certificateRepository;
    private QuizSubjectMetadataResolver resolver;

    @BeforeEach
    void setUp() {
        subjectExamDetailRepository = mock(SubjectExamDetailRepository.class);
        subjectReviewDetailRepository = mock(SubjectReviewDetailRepository.class);
        subjectOtherDetailRepository = mock(SubjectOtherDetailRepository.class);
        certificateRepository = mock(CertificateRepository.class);
        resolver = new QuizSubjectMetadataResolver(
                subjectExamDetailRepository,
                subjectReviewDetailRepository,
                subjectOtherDetailRepository,
                certificateRepository
        );
    }

    @Test
    void resolve_returns_exam_metadata_with_certificate_name() {
        Subject subject = subject(10L, "exam");
        SubjectExamDetail detail = entity(SubjectExamDetail.class);
        ReflectionTestUtils.setField(detail, "examType", "certificate");
        ReflectionTestUtils.setField(detail, "certificateId", 20L);
        Certificate certificate = entity(Certificate.class);
        ReflectionTestUtils.setField(certificate, "name", "정보처리기사");
        when(subjectExamDetailRepository.findBySubjectId(10L)).thenReturn(Optional.of(detail));
        when(certificateRepository.findById(20L)).thenReturn(Optional.of(certificate));

        Map<String, String> metadata = resolver.resolve(subject);

        assertThat(metadata).containsExactly(
                entry("시험 유형", "certificate"),
                entry("자격증명", "정보처리기사")
        );
    }

    @Test
    void resolve_returns_review_metadata() {
        Subject subject = subject(10L, "review");
        SubjectReviewDetail detail = entity(SubjectReviewDetail.class);
        ReflectionTestUtils.setField(detail, "field", "IT");
        ReflectionTestUtils.setField(detail, "studyLevel", "beginner");
        when(subjectReviewDetailRepository.findBySubjectId(10L)).thenReturn(Optional.of(detail));

        Map<String, String> metadata = resolver.resolve(subject);

        assertThat(metadata).containsExactly(
                entry("분야", "IT"),
                entry("학습 수준", "beginner")
        );
    }

    @Test
    void resolve_returns_other_metadata() {
        Subject subject = subject(10L, "other");
        SubjectOtherDetail detail = entity(SubjectOtherDetail.class);
        ReflectionTestUtils.setField(detail, "usagePurpose", "work");
        ReflectionTestUtils.setField(detail, "description", "업무 통계 복습");
        when(subjectOtherDetailRepository.findBySubjectId(10L)).thenReturn(Optional.of(detail));

        Map<String, String> metadata = resolver.resolve(subject);

        assertThat(metadata).containsExactly(
                entry("이용 목적", "work"),
                entry("추가 설명", "업무 통계 복습")
        );
    }

    private Subject subject(Long id, String purpose) {
        Subject subject = entity(Subject.class);
        ReflectionTestUtils.setField(subject, "id", id);
        ReflectionTestUtils.setField(subject, "purpose", purpose);
        return subject;
    }

    private <T> T entity(Class<T> type) {
        return BeanUtils.instantiateClass(type);
    }
}
