package com.f1.quiket.domain.lecture.repository;

import com.f1.quiket.domain.lecture.entity.LectureUploadFile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 강의 업로드 파일 메타데이터 리포지토리
 */
@Repository
public interface LectureUploadFileRepository extends JpaRepository<LectureUploadFile, Long> {

    /**
     * 업로드 파일 목록 순서 조회
     */
    List<LectureUploadFile> findAllByLectureUploadIdAndDeletedAtIsNullOrderByDisplayOrderAsc(Long lectureUploadId);
}
