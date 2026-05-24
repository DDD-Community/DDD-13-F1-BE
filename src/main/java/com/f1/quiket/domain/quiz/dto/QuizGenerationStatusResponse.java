package com.f1.quiket.domain.quiz.dto;

import com.f1.quiket.domain.quiz.entity.QuizSession;
import lombok.Builder;
import lombok.Getter;

/**
 * 퀴즈 생성 상태 응답 DTO
 */
@Getter
@Builder
public class QuizGenerationStatusResponse {

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_IN_PROGRESS = "in_progress";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_FAILED = "failed";

    private final String quizSessionId;
    private final String jobId;
    private final String status;
    private final Integer estimatedSeconds;
    private final Integer progressPct;
    private final Integer generatedCount;
    private final String failReason;

    public static QuizGenerationStatusResponse from(QuizSession quizSession) {
        // TODO: AI 생성 도입 후 estimatedSeconds 산출
        return QuizGenerationStatusResponse.builder()
                .quizSessionId(quizSession.getPublicId())
                .jobId(quizSession.getJobId())
                .status(quizSession.getStatus())
                .estimatedSeconds(null)
                .progressPct(resolveProgressPct(quizSession.getStatus()))
                .generatedCount(quizSession.getGeneratedCount())
                .failReason(quizSession.getFailReason())
                .build();
    }

    // TODO: progressPct — AI 생성 도입 후 quiz_generation_jobs.progress_pct 실제 진행률 사용. 현재는 상태별 placeholder
    private static Integer resolveProgressPct(String status) {
        return switch (status) {
            case STATUS_PENDING -> 0;
            case STATUS_IN_PROGRESS -> 50;
            case STATUS_COMPLETED, STATUS_FAILED -> 100;
            default -> 0;
        };
    }
}
