package com.f1.quiket.domain.quiz.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class QuizAnswerSubmitItem {

    @NotBlank(message = "문항 ID는 필수입니다")
    @Size(max = 36, message = "문항 ID는 36자 이하여야 합니다")
    private String questionId;

    @Size(max = 36, message = "선택지 ID는 36자 이하여야 합니다")
    private String selectedOptionId;

    @Size(max = 10, message = "선택값은 10자 이하여야 합니다")
    private String selectedValue;

    private Boolean correctClient;

    @NotNull(message = "스킵 여부는 필수입니다")
    private Boolean skipped;

    @Min(value = 0, message = "문항 풀이 시간은 0 이상이어야 합니다")
    private Integer answerElapsedMs;

    private Boolean marked;
}
