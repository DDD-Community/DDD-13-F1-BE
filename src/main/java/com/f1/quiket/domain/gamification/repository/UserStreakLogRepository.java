package com.f1.quiket.domain.gamification.repository;

import com.f1.quiket.domain.gamification.entity.UserStreakLog;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserStreakLogRepository extends JpaRepository<UserStreakLog, Long> {

    Optional<UserStreakLog> findByUserIdAndStudyDate(Long userId, LocalDate studyDate);

    Optional<UserStreakLog> findTopByUserIdAndStudyDateBeforeOrderByStudyDateDesc(Long userId, LocalDate studyDate);
}
