package com.f1.quiket.domain.subject.repository;

import com.f1.quiket.domain.subject.entity.Subject;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * 사용자 과목 페이지 조회
     */
    Page<Subject> findByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable);

    /**
     * 공개 식별자 기반 사용자 과목 조회
     */
    Optional<Subject> findByPublicIdAndUserIdAndDeletedAtIsNull(String publicId, Long userId);
    
    /**
     * 사용자 과목 단건 조회
     */
    Optional<Subject> findByPublicIdAndUserIdAndDeletedAtIsNull(String publicId, Long userId);

    /**
     * 사용자 과목 단건 조회 (PK 기반)
     */
    Optional<Subject> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);
}
