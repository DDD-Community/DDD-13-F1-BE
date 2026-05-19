package com.f1.quiket.domain.auth.repository;

import com.f1.quiket.domain.auth.entity.UserRefreshToken;
import com.f1.quiket.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRefreshTokenRepository extends JpaRepository<UserRefreshToken, Long> {

    Optional<UserRefreshToken> findByTokenHashAndDeletedAtIsNull(String tokenHash);

    List<UserRefreshToken> findAllByUserAndRevokedAtIsNullAndDeletedAtIsNull(User user);
}
