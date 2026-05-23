package com.f1.quiket.domain.subject.repository;

import com.f1.quiket.domain.subject.entity.Subject;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 과목 조회 리포지토리
 */
@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    /**
     * 사용자 과목 목록 조회
     */
    List<Subject> findAllByUserIdAndDeletedAtIsNull(Long userId);
}
