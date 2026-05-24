package com.f1.quiket.domain.quiz.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 퀴즈 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
public class QuizCreateRequest {

    @NotBlank(message = "과목 ID는 필수입니다")
    private String subjectId;

    @NotEmpty(message = "출제 범위는 최소 1개 이상 선택해야 합니다")
    private List<@NotBlank(message = "파트 ID는 비어 있을 수 없습니다") String> partIds;

    @NotBlank(message = "퀴즈 형식은 필수입니다")
    @Pattern(regexp = "multiple_choice|ox", message = "퀴즈 형식이 올바르지 않습니다")
    private String quizType;

    private Integer choiceCount;

    @NotNull(message = "문제 수는 필수입니다")
    @Min(value = 1, message = "문제 수는 1개 이상이어야 합니다")
    @Max(value = 100, message = "문제 수는 100개 이하여야 합니다")
    private Integer questionCount;

    @NotBlank(message = "풀이 방식은 필수입니다")
    @Pattern(regexp = "one_by_one|all_at_once", message = "풀이 방식이 올바르지 않습니다")
    private String playMode;

    private Boolean timerEnabled;

    @Pattern(regexp = "per_question|total", message = "타이머 범위가 올바르지 않습니다")
    private String timerScope;

    private Integer timerSeconds;

    @Pattern(regexp = "easy|medium|hard", message = "난이도가 올바르지 않습니다")
    private String difficulty;
}
