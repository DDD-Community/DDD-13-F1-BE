package com.f1.quiket.domain.subject.repository;

import com.f1.quiket.domain.subject.entity.SubjectExamDetail;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 과목 시험 상세 리포지토리
 */
@Repository
public interface SubjectExamDetailRepository extends JpaRepository<SubjectExamDetail, Long> {

    /**
     * 과목 시험 상세 조회
     */
    Optional<SubjectExamDetail> findBySubjectId(Long subjectId);

    /**
     * 과목 시험 상세 삭제
     */
    void deleteBySubjectId(Long subjectId);
}
