package com.f1.quiket.domain.chapter.repository;

import com.f1.quiket.domain.chapter.entity.Chapter;
import java.util.List;
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
}
