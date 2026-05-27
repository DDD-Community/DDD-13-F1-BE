package com.f1.quiket.domain.lecture.repository;

import com.f1.quiket.domain.lecture.entity.PartSplitPlan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 직접 분류 파트 계획 리포지토리
 */
@Repository
public interface PartSplitPlanRepository extends JpaRepository<PartSplitPlan, Long> {

    /**
     * 업로드별 직접 분류 계획 목록 조회
     */
    List<PartSplitPlan> findAllByLectureUploadIdOrderByPartNumberAsc(Long lectureUploadId);
}
