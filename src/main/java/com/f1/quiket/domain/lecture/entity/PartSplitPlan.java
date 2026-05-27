package com.f1.quiket.domain.lecture.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * 직접 분류 방식 파트 계획 엔티티
 *
 * 사용자가 지정한 파트 번호와 의도 파트명을 AI 분류 기준으로 보관
 */
@Entity
@Table(
        name = "part_split_plans",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_part_split_plans_upload_part",
                        columnNames = {"lecture_upload_id", "part_number"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PartSplitPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "lecture_upload_id", nullable = false)
    Long lectureUploadId;

    @Column(name = "part_number", nullable = false)
    Integer partNumber;

    @Column(name = "intended_name", length = 100)
    String intendedName;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    /**
     * 직접 분류 계획 생성
     */
    public static PartSplitPlan create(Long lectureUploadId, Integer partNumber, String intendedName) {
        PartSplitPlan plan = new PartSplitPlan();
        plan.lectureUploadId = lectureUploadId;
        plan.partNumber = partNumber;
        plan.intendedName = intendedName;
        plan.createdAt = LocalDateTime.now();
        return plan;
    }
}
