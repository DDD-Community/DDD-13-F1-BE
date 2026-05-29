package com.f1.quiket.domain.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class QuizRetryRequest {

    @NotBlank(message = "풀이 세션 ID는 필수입니다")
    @Size(max = 128, message = "풀이 세션 ID는 128자 이하여야 합니다")
    private String clientSessionId;

    private Boolean questionShuffled;

    private Boolean optionShuffled;

    @Size(max = 100, message = "셔플 시드는 100자 이하여야 합니다")
    private String shuffleSeed;
}
