package com.f1.quiket.domain.quiz.repository;

import com.f1.quiket.domain.quiz.entity.QuizSession;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 퀴즈 세션 조회 리포지토리
 */
@Repository
public interface QuizSessionRepository extends JpaRepository<QuizSession, Long> {

    /**
     * 사용자 퀴즈 세션 목록 조회
     */
    List<QuizSession> findAllByUserIdAndDeletedAtIsNull(Long userId);

    /**
     * 사용자 생성 중 퀴즈 존재 여부
     */
    boolean existsByUserIdAndStatusInAndDeletedAtIsNull(Long userId, Collection<String> statuses);

    /**
     * 사용자 퀴즈 세션 단건 조회
     */
    Optional<QuizSession> findByPublicIdAndUserIdAndDeletedAtIsNull(String publicId, Long userId);

    /**
     * 사용자 퀴즈 세션 단건 조회 (PK 기반)
     */
    Optional<QuizSession> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);
}
