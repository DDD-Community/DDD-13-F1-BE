package com.f1.quiket.domain.quiz.repository;

import com.f1.quiket.domain.quiz.entity.Question;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * 동일 출제 범위 최근 문항 본문 조회
     */
    @Query("""
            select question.body
            from Question question
            where question.userId = :userId
              and question.subjectId = :subjectId
              and question.partId in :partIds
              and question.deletedAt is null
            order by question.id desc
            """)
    List<String> findRecentBodiesByScope(
            @Param("userId") Long userId,
            @Param("subjectId") Long subjectId,
            @Param("partIds") Collection<Long> partIds,
            Pageable pageable
    );
}
