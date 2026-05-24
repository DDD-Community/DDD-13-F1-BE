package com.f1.quiket.domain.quiz.repository;

import com.f1.quiket.domain.quiz.entity.QuestionOption;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 퀴즈 문항 선택지 조회 리포지토리
 */
@Repository
public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {

    /**
     * 문항 선택지 목록 조회
     */
    List<QuestionOption> findAllByQuestionIdInOrderByQuestionIdAscOptionNumberAsc(Collection<Long> questionIds);
}
