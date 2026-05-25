package com.f1.quiket.domain.subject.dto;

import com.f1.quiket.domain.subject.entity.SubjectExamDetail;
import lombok.Builder;
import lombok.Getter;

/**
 * 과목 시험 상세 응답 DTO
 */
@Getter
@Builder
public class SubjectExamDetailResponse {

    /** 시험 유형 */
    private final String examType;
    /** 대학 전공 계열 */
    private final String univMajorField;
    /** 대학 전공명 */
    private final String univMajorName;
    /** 대학 과목 유형 */
    private final String univCourseType;
    /** 중고등 학년 */
    private final String mhGrade;
    /** 중고등 과목 유형 */
    private final String mhSubjectType;
    /** 자격증 식별자 */
    private final Long certificateId;
    /** 직접 입력 자격증명 */
    private final String certificateName;
    /** 공무원 급수 */
    private final String civilRank;
    /** 공무원 직렬 */
    private final String civilSeries;
    /** 어학 언어 */
    private final String langType;
    /** 어학 시험명 */
    private final String langExamName;
    /** 기타 시험명 */
    private final String otherExamName;

    /**
     * 엔티티 응답 변환
     */
    public static SubjectExamDetailResponse from(SubjectExamDetail detail) {
        return SubjectExamDetailResponse.builder()
                .examType(detail.getExamType())
                .univMajorField(detail.getUnivMajorField())
                .univMajorName(detail.getUnivMajorName())
                .univCourseType(detail.getUnivCourseType())
                .mhGrade(detail.getMhGrade())
                .mhSubjectType(detail.getMhSubjectType())
                .certificateId(detail.getCertificateId())
                .certificateName(detail.getCertificateName())
                .civilRank(detail.getCivilRank())
                .civilSeries(detail.getCivilSeries())
                .langType(detail.getLangType())
                .langExamName(detail.getLangExamName())
                .otherExamName(detail.getOtherExamName())
                .build();
    }
}
