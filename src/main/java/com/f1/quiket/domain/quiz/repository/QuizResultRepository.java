package com.f1.quiket.domain.quiz.repository;

import com.f1.quiket.domain.home.repository.SubjectLastActivityProjection;
import com.f1.quiket.domain.quiz.entity.QuizResult;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
     * 사용자 결과 최신 목록 조회
     */
    List<QuizResult> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 사용자 결과 개수
     */
    long countByUserIdAndDeletedAtIsNull(Long userId);

    /**
     * 풀이 세션 결과 단건 조회
     */
    Optional<QuizResult> findByPlaySessionId(Long playSessionId);

    /**
     * 사용자 결과 단건 조회
     */
    Optional<QuizResult> findByPublicIdAndUserId(String publicId, Long userId);

    /**
     * 과목별 결과 최신 시각 조회
     */
    @Query("""
            select result.subjectId as subjectId,
                   max(result.createdAt) as lastActivityAt
            from QuizResult result
            where result.userId = :userId
              and result.deletedAt is null
              and result.subjectId in :subjectIds
            group by result.subjectId
            """)
    List<SubjectLastActivityProjection> findLastActivityBySubjectIds(
            @Param("userId") Long userId,
            @Param("subjectIds") Collection<Long> subjectIds
    );
}
