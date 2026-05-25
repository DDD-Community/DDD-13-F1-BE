package com.f1.quiket.domain.subject.repository;

import com.f1.quiket.domain.subject.entity.SubjectExamSchedule;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 과목 시험 일정 조회 리포지토리
 */
@Repository
public interface SubjectExamScheduleRepository extends JpaRepository<SubjectExamSchedule, Long> {

    /**
     * D-Day 대상 일정 조회
     */
    List<SubjectExamSchedule> findByUserIdAndDeletedAtIsNullAndExamDateGreaterThanEqualOrderByExamDateAscCreatedAtDesc(
            Long userId,
            LocalDate examDate,
            Pageable pageable
    );

    /**
     * 과목별 일정 목록 조회
     */
    List<SubjectExamSchedule> findAllBySubjectIdInAndDeletedAtIsNull(Collection<Long> subjectIds);

    /**
     * 과목 일정 조회
     */
    Optional<SubjectExamSchedule> findBySubjectIdAndDeletedAtIsNull(Long subjectId);

    /**
     * 과목 일정 전체 조회
     */
    Optional<SubjectExamSchedule> findBySubjectId(Long subjectId);
}
