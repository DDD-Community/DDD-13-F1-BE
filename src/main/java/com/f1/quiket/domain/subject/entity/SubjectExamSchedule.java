package com.f1.quiket.domain.subject.entity;

import com.f1.quiket.global.entity.BaseEntity;
import com.f1.quiket.global.util.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * 과목 시험 일정 엔티티
 */
@Entity
@Table(
        name = "subject_exam_schedules",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_subject_exam_schedules_public_id", columnNames = "public_id"),
                @UniqueConstraint(name = "uq_subject_exam_schedules_subject_id", columnNames = "subject_id")
        },
        indexes = {
                @Index(name = "idx_subject_exam_schedules_user_id_exam_date", columnList = "user_id, exam_date"),
                @Index(name = "idx_subject_exam_schedules_exam_date", columnList = "exam_date")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubjectExamSchedule extends BaseEntity {

    @Column(name = "public_id", length = 36, nullable = false)
    String publicId;

    @Column(name = "subject_id", nullable = false)
    Long subjectId;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Column(name = "exam_name", length = 100)
    String examName;

    @Column(name = "exam_date", nullable = false)
    LocalDate examDate;

    /**
     * 시험 일정 생성
     */
    public static SubjectExamSchedule create(Long subjectId, Long userId, String examName, LocalDate examDate) {
        SubjectExamSchedule schedule = new SubjectExamSchedule();
        schedule.publicId = UuidV7Generator.generate();
        schedule.subjectId = subjectId;
        schedule.userId = userId;
        schedule.examName = examName;
        schedule.examDate = examDate;
        return schedule;
    }

    /**
     * 시험 일정 변경
     */
    public void update(String examName, LocalDate examDate) {
        this.examName = examName;
        this.examDate = examDate;
        restore();
    }
}
