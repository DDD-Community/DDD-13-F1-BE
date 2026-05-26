package com.f1.quiket.domain.subject.entity;

import com.f1.quiket.domain.subject.dto.SubjectReviewDetailRequest;
import com.f1.quiket.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * 과목 복습 상세 엔티티
 */
@Entity
@Table(
        name = "subject_review_details",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_subject_review_details_subject_id", columnNames = "subject_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubjectReviewDetail extends BaseEntity {

    @Column(name = "subject_id", nullable = false)
    Long subjectId;

    @Column(name = "field", length = 30, nullable = false)
    String field;

    @Column(name = "study_level", length = 30, nullable = false)
    String studyLevel;

    /**
     * 복습 상세 생성
     */
    public static SubjectReviewDetail create(Long subjectId, SubjectReviewDetailRequest request) {
        SubjectReviewDetail detail = new SubjectReviewDetail();
        detail.subjectId = subjectId;
        detail.update(request);
        return detail;
    }

    /**
     * 복습 상세 변경
     */
    public void update(SubjectReviewDetailRequest request) {
        this.field = request.getField();
        this.studyLevel = request.getStudyLevel();
    }
}
