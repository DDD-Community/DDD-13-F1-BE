package com.f1.quiket.domain.home.dto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.Builder;
import lombok.Getter;

/**
 * 과목 시험 일정 응답 DTO
 */
@Getter
@Builder
public class SubjectExamScheduleResponse {

    /** 일정 식별자 */
    private final String id;
    /** 과목 식별자 */
    private final String subjectId;
    /** 시험명 */
    private final String examName;
    /** 시험일 */
    private final LocalDate examDate;
    /** D-Day */
    private final Integer dDay;

    /**
     * 일정 응답 생성
     */
    public static SubjectExamScheduleResponse of(
            String id,
            String subjectId,
            String subjectName,
            String examName,
            LocalDate examDate
    ) {
        return SubjectExamScheduleResponse.builder()
                .id(id)
                .subjectId(subjectId)
                .examName(examName == null || examName.isBlank() ? subjectName : examName)
                .examDate(examDate)
                .dDay(calculateDDay(examDate))
                .build();
    }

    /**
     * D-Day 산출
     */
    private static Integer calculateDDay(LocalDate examDate) {
        long remainDays = ChronoUnit.DAYS.between(LocalDate.now(), examDate);
        // 지난 시험일 제외
        if (remainDays < 0) {
            return null;
        }
        return Math.toIntExact(remainDays);
    }
}
