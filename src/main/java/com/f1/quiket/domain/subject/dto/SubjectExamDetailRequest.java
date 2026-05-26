package com.f1.quiket.domain.subject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 과목 시험 상세 요청 DTO
 */
@Getter
@NoArgsConstructor
public class SubjectExamDetailRequest {

    /** 시험 유형 */
    @NotBlank(message = "시험 유형은 필수입니다")
    private String examType;

    /** 대학 전공 계열 */
    @Size(max = 30, message = "전공 계열은 30자 이하여야 합니다")
    private String univMajorField;

    /** 대학 전공명 */
    @Size(max = 30, message = "전공명은 30자 이하여야 합니다")
    private String univMajorName;

    /** 대학 과목 유형 */
    private String univCourseType;

    /** 중고등 학년 */
    private String mhGrade;

    /** 중고등 과목 유형 */
    @Size(max = 30, message = "과목 유형은 30자 이하여야 합니다")
    private String mhSubjectType;

    /** 자격증 식별자 */
    private Long certificateId;

    /** 직접 입력 자격증명 */
    @Size(max = 100, message = "자격증명은 100자 이하여야 합니다")
    private String certificateName;

    /** 공무원 급수 */
    private String civilRank;

    /** 공무원 직렬 */
    private String civilSeries;

    /** 어학 언어 */
    private String langType;

    /** 어학 시험명 */
    @Size(max = 30, message = "어학 시험명은 30자 이하여야 합니다")
    private String langExamName;

    /** 기타 시험명 */
    @Size(max = 30, message = "기타 시험명은 30자 이하여야 합니다")
    private String otherExamName;
}
