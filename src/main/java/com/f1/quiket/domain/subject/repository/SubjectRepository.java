package com.f1.quiket.domain.subject.repository;

import com.f1.quiket.domain.subject.entity.Subject;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 과목 조회 리포지토리
 */
@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    /**
     * 사용자 과목 목록 조회
     */
    List<Subject> findAllByUserIdAndDeletedAtIsNull(Long userId);

    /**
     * 사용자 과목 페이지 조회
     */
    Page<Subject> findByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable);

    /**
     * 홈 과목 요약 대상 조회
     */
    @Query(value = """
            SELECT s.*
            FROM subjects s
            WHERE s.user_id = :userId
              AND s.deleted_at IS NULL
            ORDER BY GREATEST(
                COALESCE((
                    SELECT MAX(qs.created_at)
                    FROM quiz_sessions qs
                    WHERE qs.user_id = :userId
                      AND qs.subject_id = s.id
                      AND qs.deleted_at IS NULL
                ), TIMESTAMP('1000-01-01 00:00:00')),
                COALESCE((
                    SELECT MAX(qps.updated_at)
                    FROM quiz_play_sessions qps
                    WHERE qps.user_id = :userId
                      AND qps.subject_id = s.id
                      AND qps.deleted_at IS NULL
                ), TIMESTAMP('1000-01-01 00:00:00')),
                COALESCE((
                    SELECT MAX(qr.created_at)
                    FROM quiz_results qr
                    WHERE qr.user_id = :userId
                      AND qr.subject_id = s.id
                      AND qr.deleted_at IS NULL
                ), TIMESTAMP('1000-01-01 00:00:00'))
            ) DESC,
            s.created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Subject> findHomeSummarySubjects(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 공개 식별자 기반 사용자 과목 조회
     */
    Optional<Subject> findByPublicIdAndUserIdAndDeletedAtIsNull(String publicId, Long userId);

    /**
     * 사용자 과목 단건 조회 (PK 기반)
     */
    Optional<Subject> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);

    /**
     * 사용자 과목 목록 조회 (PK 컬렉션 기반)
     */
    List<Subject> findAllByIdInAndUserIdAndDeletedAtIsNull(Collection<Long> ids, Long userId);
}
