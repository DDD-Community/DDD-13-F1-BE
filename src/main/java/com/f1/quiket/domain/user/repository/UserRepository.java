package com.f1.quiket.domain.user.repository;

import com.f1.quiket.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 사용자 조회 전용 리포지토리
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 활성화된 사용자 목록 조회.
     */
    List<User> findAllByStatusAndDeletedAtIsNull(String status);

    boolean existsByEmail(String email);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByPublicIdAndDeletedAtIsNull(String publicId);
}
