package com.f1.quiket.domain.quiz.repository;

import com.f1.quiket.domain.quiz.entity.QuizResult;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 퀴즈 결과 조회 리포지토리
 */
@Repository
public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {

    /**
     * 사용자 결과 목록 조회
     */
    List<QuizResult> findAllByUserId(Long userId);

    /**
     * 풀이 세션 결과 단건 조회
     */
    Optional<QuizResult> findByPlaySessionId(Long playSessionId);
}
