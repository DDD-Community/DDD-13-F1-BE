package com.f1.quiket.domain.part.repository;

import com.f1.quiket.domain.part.entity.Part;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
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
    List<Part> findAllBySubjectIdAndDeletedAtIsNull(Long subjectId);

    /**
     * 챕터별 파트 목록 조회
     */
    List<Part> findAllByChapterIdInAndDeletedAtIsNull(Collection<Long> chapterIds);

    /**
     * 과목별 파트 목록 조회
     */
    List<Part> findAllBySubjectIdInAndDeletedAtIsNull(Collection<Long> subjectIds);
}
