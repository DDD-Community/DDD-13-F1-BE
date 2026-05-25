package com.f1.quiket.domain.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class QuizResultSubmitRequest {

    @NotBlank(message = "풀이 세션 ID는 필수입니다")
    @Size(max = 36, message = "풀이 세션 ID는 36자 이하여야 합니다")
    private String clientSessionId;

    @NotBlank(message = "퀴즈 세션 ID는 필수입니다")
    @Size(max = 36, message = "퀴즈 세션 ID는 36자 이하여야 합니다")
    private String quizSessionId;

    @NotBlank(message = "풀이 유형은 필수입니다")
    @Pattern(regexp = "first|retry_all|retry_wrong", message = "풀이 유형이 올바르지 않습니다")
    private String playType;

    @Size(max = 36, message = "부모 풀이 세션 ID는 36자 이하여야 합니다")
    private String parentPlaySessionId;

    @NotNull(message = "풀이 시간은 필수입니다")
    @Min(value = 0, message = "풀이 시간은 0 이상이어야 합니다")
    private Integer elapsedMs;

    private Boolean questionShuffled;

    private Boolean optionShuffled;

    @Size(max = 100, message = "셔플 시드는 100자 이하여야 합니다")
    private String shuffleSeed;

    @Valid
    @NotEmpty(message = "답안 목록은 필수입니다")
    private List<QuizAnswerSubmitItem> answers;
}
