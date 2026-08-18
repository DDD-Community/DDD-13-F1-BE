package com.f1.quiket.domain.quiz.service;

import com.f1.quiket.domain.subject.entity.Certificate;
import com.f1.quiket.domain.subject.entity.Subject;
import com.f1.quiket.domain.subject.entity.SubjectExamDetail;
import com.f1.quiket.domain.subject.entity.SubjectOtherDetail;
import com.f1.quiket.domain.subject.entity.SubjectReviewDetail;
import com.f1.quiket.domain.subject.entity.type.SubjectPurpose;
import com.f1.quiket.domain.subject.repository.CertificateRepository;
import com.f1.quiket.domain.subject.repository.SubjectExamDetailRepository;
import com.f1.quiket.domain.subject.repository.SubjectOtherDetailRepository;
import com.f1.quiket.domain.subject.repository.SubjectReviewDetailRepository;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class QuizSubjectMetadataResolver {

    private final SubjectExamDetailRepository subjectExamDetailRepository;
    private final SubjectReviewDetailRepository subjectReviewDetailRepository;
    private final SubjectOtherDetailRepository subjectOtherDetailRepository;
    private final CertificateRepository certificateRepository;

    public Map<String, String> resolve(Subject subject) {
        return switch (SubjectPurpose.from(subject.getPurpose())) {
            case EXAM -> subjectExamDetailRepository.findBySubjectId(subject.getId())
                    .map(this::examMetadata)
                    .orElseGet(Map::of);
            case REVIEW -> subjectReviewDetailRepository.findBySubjectId(subject.getId())
                    .map(this::reviewMetadata)
                    .orElseGet(Map::of);
            case OTHER -> subjectOtherDetailRepository.findBySubjectId(subject.getId())
                    .map(this::otherMetadata)
                    .orElseGet(Map::of);
        };
    }

    private Map<String, String> examMetadata(SubjectExamDetail detail) {
        Map<String, String> metadata = new LinkedHashMap<>();
        putIfPresent(metadata, "시험 유형", detail.getExamType());
        putIfPresent(metadata, "전공 계열", detail.getUnivMajorField());
        putIfPresent(metadata, "전공명", detail.getUnivMajorName());
        putIfPresent(metadata, "대학 과목 유형", detail.getUnivCourseType());
        putIfPresent(metadata, "학년", detail.getMhGrade());
        putIfPresent(metadata, "중고등 과목 유형", detail.getMhSubjectType());
        putIfPresent(metadata, "자격증명", resolveCertificateName(detail));
        putIfPresent(metadata, "공무원 급수", detail.getCivilRank());
        putIfPresent(metadata, "공무원 직렬", detail.getCivilSeries());
        putIfPresent(metadata, "언어", detail.getLangType());
        putIfPresent(metadata, "어학 시험명", detail.getLangExamName());
        putIfPresent(metadata, "기타 시험명", detail.getOtherExamName());
        return immutableMetadata(metadata);
    }

    private Map<String, String> reviewMetadata(SubjectReviewDetail detail) {
        Map<String, String> metadata = new LinkedHashMap<>();
        putIfPresent(metadata, "분야", detail.getField());
        putIfPresent(metadata, "학습 수준", detail.getStudyLevel());
        return immutableMetadata(metadata);
    }

    private Map<String, String> otherMetadata(SubjectOtherDetail detail) {
        Map<String, String> metadata = new LinkedHashMap<>();
        putIfPresent(metadata, "이용 목적", detail.getUsagePurpose());
        putIfPresent(metadata, "추가 설명", detail.getDescription());
        return immutableMetadata(metadata);
    }

    private String resolveCertificateName(SubjectExamDetail detail) {
        if (StringUtils.hasText(detail.getCertificateName())) {
            return detail.getCertificateName();
        }
        if (detail.getCertificateId() == null) {
            return null;
        }
        return certificateRepository.findById(detail.getCertificateId())
                .map(Certificate::getName)
                .orElse(null);
    }

    private void putIfPresent(Map<String, String> metadata, String key, String value) {
        if (StringUtils.hasText(value)) {
            metadata.put(key, value);
        }
    }

    private Map<String, String> immutableMetadata(Map<String, String> metadata) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}
