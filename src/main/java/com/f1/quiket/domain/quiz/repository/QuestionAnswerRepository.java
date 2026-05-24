package com.f1.quiket.domain.quiz.repository;

import com.f1.quiket.domain.quiz.entity.QuestionAnswer;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 퀴즈 문항 정답 조회 리포지토리
 */
@Repository
public interface QuestionAnswerRepository extends JpaRepository<QuestionAnswer, Long> {

    /**
     * 문항 정답 목록 조회
     */
    List<QuestionAnswer> findAllByQuestionIdIn(Collection<Long> questionIds);
}
