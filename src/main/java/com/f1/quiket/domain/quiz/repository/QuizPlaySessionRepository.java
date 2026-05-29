package com.f1.quiket.domain.quiz.repository;

import com.f1.quiket.domain.home.repository.SubjectLastActivityProjection;
import com.f1.quiket.domain.quiz.entity.QuizPlaySession;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * 사용자 풀이 세션 상태별 최신 목록 조회
     */
    List<QuizPlaySession> findByUserIdAndStatusAndDeletedAtIsNullOrderByUpdatedAtDesc(
            Long userId,
            String status,
            Pageable pageable
    );

    /**
     * 사용자 풀이 세션 상태별 개수
     */
    long countByUserIdAndStatusAndDeletedAtIsNull(Long userId, String status);

    /**
     * 클라이언트 풀이 세션 단건 조회
     */
    Optional<QuizPlaySession> findByClientSessionId(String clientSessionId);

    /**
     * 사용자 풀이 세션 단건 조회
     */
    Optional<QuizPlaySession> findByIdAndUserId(Long id, Long userId);

    /**
     * 사용자 풀이 세션 목록 조회 (PK 컬렉션 기반)
     */
    List<QuizPlaySession> findAllByIdInAndUserIdAndDeletedAtIsNull(Collection<Long> ids, Long userId);

    /**
     * 과목별 풀이 최신 시각 조회
     */
    @Query("""
            select playSession.subjectId as subjectId,
                   max(playSession.updatedAt) as lastActivityAt
            from QuizPlaySession playSession
            where playSession.userId = :userId
              and playSession.deletedAt is null
              and playSession.subjectId in :subjectIds
            group by playSession.subjectId
            """)
    List<SubjectLastActivityProjection> findLastActivityBySubjectIds(
            @Param("userId") Long userId,
            @Param("subjectIds") Collection<Long> subjectIds
    );
}
