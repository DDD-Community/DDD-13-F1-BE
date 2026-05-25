package com.f1.quiket.domain.quiz.repository;

import com.f1.quiket.domain.quiz.entity.QuizGenerationJob;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizGenerationJobRepository extends JpaRepository<QuizGenerationJob, Long> {

    Optional<QuizGenerationJob> findByQuizSessionId(Long quizSessionId);

    List<QuizGenerationJob> findAllByStatusAndTimeoutAtBefore(String status, LocalDateTime timeoutAt);
}
