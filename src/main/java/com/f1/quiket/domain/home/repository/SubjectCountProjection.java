package com.f1.quiket.domain.home.repository;

/**
 * 과목별 개수 집계 조회
 */
public interface SubjectCountProjection {

    Long getSubjectId();

    Long getItemCount();
}
