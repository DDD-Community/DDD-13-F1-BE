package com.f1.quiket.domain.quiz.repository;

import com.f1.quiket.domain.quiz.entity.QuizSessionScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 퀴즈 세션 출제 범위 리포지토리
 */
@Repository
public interface QuizSessionScopeRepository extends JpaRepository<QuizSessionScope, Long> {
}
