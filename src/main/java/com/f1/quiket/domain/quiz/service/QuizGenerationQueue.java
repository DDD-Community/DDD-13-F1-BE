package com.f1.quiket.domain.quiz.service;

public interface QuizGenerationQueue {

    String enqueue(QuizGenerationQueueMessage message);
}
