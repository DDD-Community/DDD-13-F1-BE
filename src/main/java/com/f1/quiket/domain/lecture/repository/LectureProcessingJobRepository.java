package com.f1.quiket.domain.lecture.repository;

import com.f1.quiket.domain.lecture.entity.LectureProcessingJob;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 강의 업로드 처리 작업 조회 리포지토리
 */
@Repository
public interface LectureProcessingJobRepository extends JpaRepository<LectureProcessingJob, Long> {

    /**
     * 업로드 식별자 기반 처리 작업 조회
     */
    Optional<LectureProcessingJob> findByLectureUploadId(Long lectureUploadId);
}
