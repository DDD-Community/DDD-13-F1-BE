package com.f1.quiket.domain.subject.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 시험 일정 저장 요청 DTO
 */
@Getter
@NoArgsConstructor
public class SubjectExamScheduleUpsertRequest {

    /** 시험명 */
    @Size(max = 100, message = "시험명은 100자 이하여야 합니다")
    private String examName;

    /** 시험일 */
    @NotNull(message = "시험 날짜는 필수입니다")
    private LocalDate examDate;
}
