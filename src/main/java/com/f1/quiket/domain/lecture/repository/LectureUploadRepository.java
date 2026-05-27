package com.f1.quiket.domain.lecture.repository;

import com.f1.quiket.domain.lecture.entity.LectureUpload;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 강의 업로드 조회 리포지토리
 */
@Repository
public interface LectureUploadRepository extends JpaRepository<LectureUpload, Long> {

    /**
     * 공개 식별자 기반 사용자 업로드 조회
     */
    Optional<LectureUpload> findByPublicIdAndUserIdAndDeletedAtIsNull(String publicId, Long userId);
}
