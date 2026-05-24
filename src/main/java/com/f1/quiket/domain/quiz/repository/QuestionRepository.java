package com.f1.quiket.domain.quiz.repository;

import com.f1.quiket.domain.quiz.entity.Question;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 퀴즈 문항 조회 리포지토리
 */
@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    /**
     * 퀴즈 세션 문항 목록 조회
     */
    List<Question> findAllByQuizSessionIdAndUserIdOrderByDisplayOrderAscIdAsc(Long quizSessionId, Long userId);
}
