package com.f1.quiket.domain.chapter.repository;

import com.f1.quiket.domain.chapter.entity.Chapter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 챕터 조회 리포지토리
 */
@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    /**
     * 사용자 챕터 목록 조회
     */
    List<Chapter> findAllByUserIdAndDeletedAtIsNull(Long userId);

    /**
     * 과목 챕터 목록 조회
     */
    List<Chapter> findAllBySubjectIdAndUserIdAndDeletedAtIsNullOrderByDisplayOrderAscCreatedAtAsc(
            Long subjectId,
            Long userId
    );

    /**
     * 과목별 챕터 목록 조회
     */
    List<Chapter> findAllBySubjectIdInAndDeletedAtIsNull(Collection<Long> subjectIds);

    /**
     * 공개 식별자 기반 사용자 챕터 조회
     */
    Optional<Chapter> findByPublicIdAndUserIdAndDeletedAtIsNull(String publicId, Long userId);

    /**
     * 사용자 챕터 목록 조회 (PK 컬렉션 기반)
     */
    List<Chapter> findAllByIdInAndUserIdAndDeletedAtIsNull(Collection<Long> ids, Long userId);

    /**
     * 과목 내 마지막 챕터 순서 조회
     */
    @Query("""
            select coalesce(max(c.displayOrder), 0)
            from Chapter c
            where c.subjectId = :subjectId
              and c.deletedAt is null
            """)
    int findMaxDisplayOrderBySubjectId(@Param("subjectId") Long subjectId);
}
