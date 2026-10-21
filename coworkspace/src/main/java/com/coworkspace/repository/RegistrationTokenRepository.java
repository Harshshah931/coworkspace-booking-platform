package com.coworkspace.repository;

import com.coworkspace.entity.RegistrationToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RegistrationTokenRepository extends JpaRepository<RegistrationToken, Long> {

    Optional<RegistrationToken> findByTokenAndConsumedFalse(String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RegistrationToken r WHERE r.token = :token")
    Optional<RegistrationToken> findByTokenWithLock(@Param("token") String token);

    void deleteByExpiresAtBefore(Instant expiry);
}
