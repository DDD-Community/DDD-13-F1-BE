package com.f1.quiket.domain.quiz.service;

import java.util.Optional;

public interface QuizGenerationQueue {

    String enqueue(QuizGenerationQueueMessage message);

    Optional<QuizGenerationQueueRecord> poll();

    void acknowledge(String messageId);
}
