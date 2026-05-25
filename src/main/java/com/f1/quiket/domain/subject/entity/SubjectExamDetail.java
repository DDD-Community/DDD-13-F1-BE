package com.f1.quiket.domain.subject.entity;

import com.f1.quiket.domain.subject.dto.SubjectExamDetailRequest;
import com.f1.quiket.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * 과목 시험 상세 엔티티
 */
@Entity
@Table(
        name = "subject_exam_details",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_subject_exam_details_subject_id", columnNames = "subject_id")
        },
        indexes = {
                @Index(name = "idx_subject_exam_details_exam_type", columnList = "exam_type")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubjectExamDetail extends BaseEntity {

    @Column(name = "subject_id", nullable = false)
    Long subjectId;

    @Column(name = "exam_type", length = 20, nullable = false)
    String examType;

    @Column(name = "univ_major_field", length = 30)
    String univMajorField;

    @Column(name = "univ_major_name", length = 30)
    String univMajorName;

    @Column(name = "univ_course_type", length = 20)
    String univCourseType;

    @Column(name = "mh_grade", length = 10)
    String mhGrade;

    @Column(name = "mh_subject_type", length = 30)
    String mhSubjectType;

    @Column(name = "certificate_id")
    Long certificateId;

    @Column(name = "certificate_name", length = 100)
    String certificateName;

    @Column(name = "civil_rank", length = 20)
    String civilRank;

    @Column(name = "civil_series", length = 30)
    String civilSeries;

    @Column(name = "lang_type", length = 20)
    String langType;

    @Column(name = "lang_exam_name", length = 30)
    String langExamName;

    @Column(name = "other_exam_name", length = 30)
    String otherExamName;

    /**
     * 시험 상세 생성
     */
    public static SubjectExamDetail create(Long subjectId, SubjectExamDetailRequest request) {
        SubjectExamDetail detail = new SubjectExamDetail();
        detail.subjectId = subjectId;
        detail.update(request);
        return detail;
    }

    /**
     * 시험 상세 변경
     */
    public void update(SubjectExamDetailRequest request) {
        this.examType = request.getExamType();
        this.univMajorField = request.getUnivMajorField();
        this.univMajorName = request.getUnivMajorName();
        this.univCourseType = request.getUnivCourseType();
        this.mhGrade = request.getMhGrade();
        this.mhSubjectType = request.getMhSubjectType();
        this.certificateId = request.getCertificateId();
        this.certificateName = request.getCertificateName();
        this.civilRank = request.getCivilRank();
        this.civilSeries = request.getCivilSeries();
        this.langType = request.getLangType();
        this.langExamName = request.getLangExamName();
        this.otherExamName = request.getOtherExamName();
    }
}
