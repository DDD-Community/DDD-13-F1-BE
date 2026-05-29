package com.f1.quiket.domain.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class QuizPlayStartRequest {

    @NotBlank(message = "풀이 세션 ID는 필수입니다")
    @Size(max = 128, message = "풀이 세션 ID는 128자 이하여야 합니다")
    private String clientSessionId;

    @NotBlank(message = "풀이 유형은 필수입니다")
    @Pattern(regexp = "first|retry_all|retry_wrong", message = "풀이 유형이 올바르지 않습니다")
    private String playType;

    @Size(max = 128, message = "부모 풀이 세션 ID는 128자 이하여야 합니다")
    private String parentPlaySessionId;

    private Boolean questionShuffled;

    private Boolean optionShuffled;

    @Size(max = 100, message = "셔플 시드는 100자 이하여야 합니다")
    private String shuffleSeed;
}
