package com.f1.quiket.domain.chapter.repository;

import com.f1.quiket.domain.chapter.entity.Chapter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
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
    List<Chapter> findAllBySubjectIdAndDeletedAtIsNullOrderByDisplayOrderAscCreatedAtAsc(Long subjectId);

    /**
     * 과목별 챕터 목록 조회
     */
    List<Chapter> findAllBySubjectIdInAndDeletedAtIsNull(Collection<Long> subjectIds);

    /**
     * 공개 식별자 기반 사용자 챕터 조회
     */
    Optional<Chapter> findByPublicIdAndUserIdAndDeletedAtIsNull(String publicId, Long userId);
}
