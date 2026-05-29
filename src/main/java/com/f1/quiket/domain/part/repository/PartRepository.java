package com.f1.quiket.domain.part.repository;

import com.f1.quiket.domain.part.entity.Part;
import com.f1.quiket.domain.home.repository.SubjectCountProjection;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
     * 공개 식별자 기반 사용자 파트 조회
     */
    Optional<Part> findByPublicIdAndUserIdAndContentDeletedFalseAndDeletedAtIsNull(String publicId, Long userId);

    /**
     * 사용자 파트 목록 조회
     */
    List<Part> findAllByUserIdAndDeletedAtIsNull(Long userId);

    /**
     * 과목 파트 목록 조회
     */
    List<Part> findAllBySubjectIdAndDeletedAtIsNull(Long subjectId);

    /**
     * 챕터별 파트 목록 조회
     */
    List<Part> findAllByChapterIdInAndDeletedAtIsNull(Collection<Long> chapterIds);

    /**
     * 과목별 파트 목록 조회
     */
    List<Part> findAllBySubjectIdInAndDeletedAtIsNull(Collection<Long> subjectIds);

    /**
     * 과목별 파트 개수 집계
     */
    @Query("""
            select p.subjectId as subjectId,
                   count(p) as itemCount
            from Part p
            where p.userId = :userId
              and p.deletedAt is null
              and p.subjectId in :subjectIds
            group by p.subjectId
            """)
    List<SubjectCountProjection> countBySubjectIds(
            @Param("userId") Long userId,
            @Param("subjectIds") Collection<Long> subjectIds
    );
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

    /**
     * 사용자 파트 목록 조회 (PK 컬렉션 기반)
     */
    List<Part> findAllByIdInAndUserIdAndDeletedAtIsNull(Collection<Long> ids, Long userId);

    /**
     * 업로드 기반 생성 파트 목록 조회
     */
    List<Part> findAllByLectureUploadIdAndUserIdAndDeletedAtIsNullOrderByPartNumberAsc(
            Long lectureUploadId,
            Long userId
    );

    /**
     * 챕터 내 마지막 파트 번호 조회
     */
    @Query("""
            select coalesce(max(p.partNumber), 0)
            from Part p
            where p.chapterId = :chapterId
              and p.deletedAt is null
            """)
    int findMaxPartNumberByChapterId(@Param("chapterId") Long chapterId);
}
