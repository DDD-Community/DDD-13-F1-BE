package com.f1.quiket.domain.subject.entity;

import com.f1.quiket.domain.subject.dto.SubjectOtherDetailRequest;
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
 * 과목 기타 상세 엔티티
 */
@Entity
@Table(
        name = "subject_other_details",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_subject_other_details_subject_id", columnNames = "subject_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubjectOtherDetail extends BaseEntity {

    @Column(name = "subject_id", nullable = false)
    Long subjectId;

    @Column(name = "usage_purpose", length = 30, nullable = false)
    String usagePurpose;

    @Column(name = "description", length = 100)
    String description;

    /**
     * 기타 상세 생성
     */
    public static SubjectOtherDetail create(Long subjectId, SubjectOtherDetailRequest request) {
        SubjectOtherDetail detail = new SubjectOtherDetail();
        detail.subjectId = subjectId;
        detail.update(request);
        return detail;
    }

    /**
     * 기타 상세 변경
     */
    public void update(SubjectOtherDetailRequest request) {
        this.usagePurpose = request.getUsagePurpose();
        this.description = request.getDescription();
    }
}
