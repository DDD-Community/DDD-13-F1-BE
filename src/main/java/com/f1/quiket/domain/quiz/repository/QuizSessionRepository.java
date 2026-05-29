package com.f1.quiket.domain.quiz.repository;

import com.f1.quiket.domain.home.repository.SubjectLastActivityProjection;
import com.f1.quiket.domain.quiz.entity.QuizSession;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 퀴즈 세션 조회 리포지토리
 */
@Repository
public interface QuizSessionRepository extends JpaRepository<QuizSession, Long> {

    /**
     * 사용자 퀴즈 세션 목록 조회
     */
    List<QuizSession> findAllByUserIdAndDeletedAtIsNull(Long userId);

    /**
     * 홈 미시작 퀴즈 활동 조회
     */
    @Query("""
            select session
            from QuizSession session
            where session.userId = :userId
              and session.status = :status
              and session.deletedAt is null
              and not exists (
                  select 1
                  from QuizPlaySession playSession
                  where playSession.userId = :userId
                    and playSession.quizSessionId = session.id
                    and playSession.deletedAt is null
              )
            order by session.completedAt desc, session.updatedAt desc
            """)
    List<QuizSession> findReadyActivities(
            @Param("userId") Long userId,
            @Param("status") String status,
            Pageable pageable
    );

    /**
     * 홈 미시작 퀴즈 활동 개수
     */
    @Query("""
            select count(session)
            from QuizSession session
            where session.userId = :userId
              and session.status = :status
              and session.deletedAt is null
              and not exists (
                  select 1
                  from QuizPlaySession playSession
                  where playSession.userId = :userId
                    and playSession.quizSessionId = session.id
                    and playSession.deletedAt is null
              )
            """)
    long countReadyActivities(@Param("userId") Long userId, @Param("status") String status);

    /**
     * 과목 퀴즈 세션 목록 조회
     */
    List<QuizSession> findAllBySubjectIdAndDeletedAtIsNull(Long subjectId);
    
    /**
     * 사용자 생성 중 퀴즈 존재 여부
     */
    boolean existsByUserIdAndStatusInAndDeletedAtIsNull(Long userId, Collection<String> statuses);

    /**
     * 사용자 퀴즈 세션 단건 조회
     */
    Optional<QuizSession> findByPublicIdAndUserIdAndDeletedAtIsNull(String publicId, Long userId);

    /**
     * 사용자 퀴즈 세션 단건 조회 (PK 기반)
     */
    Optional<QuizSession> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);

    /**
     * 사용자 퀴즈 세션 목록 조회 (PK 컬렉션 기반)
     */
    List<QuizSession> findAllByIdInAndUserIdAndDeletedAtIsNull(Collection<Long> ids, Long userId);

    /**
     * 과목별 퀴즈 생성 최신 시각 조회
     */
    @Query("""
            select session.subjectId as subjectId,
                   max(session.createdAt) as lastActivityAt
            from QuizSession session
            where session.userId = :userId
              and session.deletedAt is null
              and session.subjectId in :subjectIds
            group by session.subjectId
            """)
    List<SubjectLastActivityProjection> findLastActivityBySubjectIds(
            @Param("userId") Long userId,
            @Param("subjectIds") Collection<Long> subjectIds
    );
}
