package com.f1.quiket.domain.quiz.repository;

import com.f1.quiket.domain.quiz.entity.QuizPlayAnswer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 퀴즈 풀이 답안 조회 리포지토리
 */
@Repository
public interface QuizPlayAnswerRepository extends JpaRepository<QuizPlayAnswer, Long> {

    /**
     * 풀이 세션 답안 목록 조회
     */
    List<QuizPlayAnswer> findAllByPlaySessionId(Long playSessionId);
}
