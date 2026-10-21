package com.coworkspace.repository;

import com.coworkspace.entity.OtpChallenge;
import com.coworkspace.entity.OtpPurpose;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, Long> {

    Optional<OtpChallenge> findFirstByPhoneAndPurposeAndConsumedFalseAndSupersededFalseOrderByCreatedAtDesc(
            String phone, OtpPurpose purpose
    );

    Optional<OtpChallenge> findFirstByPhoneAndConsumedFalseAndSupersededFalseOrderByCreatedAtDesc(String phone);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM OtpChallenge c WHERE c.id = :id")
    Optional<OtpChallenge> findByIdWithLock(@Param("id") Long id);

    @Modifying
    @Query("""
            UPDATE OtpChallenge c
            SET c.superseded = true
            WHERE c.phone = :phone
              AND c.purpose = :purpose
              AND c.consumed = false
              AND c.superseded = false
            """)
    int supersedeActiveChallenges(@Param("phone") String phone, @Param("purpose") OtpPurpose purpose);

    long countByPhoneAndCreatedAtAfter(String phone, Instant after);

    long countByClientIpAndCreatedAtAfter(String clientIp, Instant after);

    Optional<OtpChallenge> findFirstByPhoneOrderByCreatedAtDesc(String phone);
}
