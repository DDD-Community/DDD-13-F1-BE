package com.f1.quiket.domain.home.repository;

import java.time.LocalDateTime;

/**
 * 과목별 마지막 활동 시각 조회
 */
public interface SubjectLastActivityProjection {

    Long getSubjectId();

    LocalDateTime getLastActivityAt();
}
