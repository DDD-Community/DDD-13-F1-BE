package com.f1.quiket.domain.subject.repository;

import com.f1.quiket.domain.subject.entity.SubjectOtherDetail;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 과목 기타 상세 리포지토리
 */
@Repository
public interface SubjectOtherDetailRepository extends JpaRepository<SubjectOtherDetail, Long> {

    /**
     * 과목 기타 상세 조회
     */
    Optional<SubjectOtherDetail> findBySubjectId(Long subjectId);

    /**
     * 과목 기타 상세 삭제
     */
    void deleteBySubjectId(Long subjectId);
}
