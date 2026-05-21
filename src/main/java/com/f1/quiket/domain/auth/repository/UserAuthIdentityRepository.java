package com.f1.quiket.domain.auth.repository;

import com.f1.quiket.domain.auth.entity.UserAuthIdentity;
import com.f1.quiket.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAuthIdentityRepository extends JpaRepository<UserAuthIdentity, Long> {

    Optional<UserAuthIdentity> findByUserAndProviderAndDeletedAtIsNull(User user, String provider);

    Optional<UserAuthIdentity> findByProviderAndProviderSubjectAndDeletedAtIsNull(String provider, String providerSubject);

    List<UserAuthIdentity> findAllByUserAndDeletedAtIsNull(User user);
}
