package com.f1.quiket.domain.quiz.repository;

import com.f1.quiket.domain.quiz.entity.QuizPlaySession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 퀴즈 풀이 세션 조회 리포지토리
 */
@Repository
public interface QuizPlaySessionRepository extends JpaRepository<QuizPlaySession, Long> {

    /**
     * 사용자 풀이 세션 목록 조회
     */
    List<QuizPlaySession> findAllByUserId(Long userId);

}
