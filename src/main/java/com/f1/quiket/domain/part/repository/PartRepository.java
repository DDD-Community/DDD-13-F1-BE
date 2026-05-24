package com.f1.quiket.domain.part.repository;

import com.f1.quiket.domain.part.entity.Part;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 파트 조회 리포지토리
 */
@Repository
public interface PartRepository extends JpaRepository<Part, Long> {

    /**
     * 사용자 파트 목록 조회
     */
    List<Part> findAllByUserIdAndDeletedAtIsNull(Long userId);

    /**
     * 과목 파트 목록 조회
     */
    List<Part> findAllBySubjectIdAndUserIdAndDeletedAtIsNullOrderByChapterIdAscPartNumberAscCreatedAtAsc(
            Long subjectId,
            Long userId
    );

    /**
     * 과목 파트 목록 조회
     */
    List<Part> findAllByPublicIdInAndSubjectIdAndUserIdAndDeletedAtIsNull(
            Collection<String> publicIds,
            Long subjectId,
            Long userId
    );

    /**
     * 출제 범위 파트 본문 길이 합계 (50자당 1문제 정책 검증용)
     */
    @Query("""
            select coalesce(sum(length(p.content)), 0)
            from Part p
            where p.publicId in :publicIds
              and p.subjectId = :subjectId
              and p.userId = :userId
              and p.deletedAt is null
            """)
    long sumContentLengthByPublicIds(
            @Param("publicIds") Collection<String> publicIds,
            @Param("subjectId") Long subjectId,
            @Param("userId") Long userId
    );
}
