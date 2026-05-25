package com.f1.quiket.domain.subject.dto;

import com.f1.quiket.domain.subject.entity.Subject;
import com.f1.quiket.domain.subject.entity.SubjectExamSchedule;
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
     * 엔티티 응답 변환
     */
    public static SubjectExamScheduleResponse of(SubjectExamSchedule schedule, Subject subject) {
        String examName = schedule.getExamName();
        return SubjectExamScheduleResponse.builder()
                .id(schedule.getPublicId())
                .subjectId(subject.getPublicId())
                .examName(examName == null || examName.isBlank() ? subject.getName() : examName)
                .examDate(schedule.getExamDate())
                .dDay(calculateDDay(schedule.getExamDate()))
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
