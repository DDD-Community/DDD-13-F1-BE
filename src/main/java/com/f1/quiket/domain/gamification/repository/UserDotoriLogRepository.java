package com.f1.quiket.domain.gamification.repository;

import com.f1.quiket.domain.gamification.entity.UserDotoriLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDotoriLogRepository extends JpaRepository<UserDotoriLog, Long> {
}
