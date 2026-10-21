package com.coworkspace.repository;

import com.coworkspace.entity.SeatUnit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeatUnitRepository extends JpaRepository<SeatUnit, Long> {
    List<SeatUnit> findBySpaceId(Long spaceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SeatUnit s WHERE s.id = :id")
    Optional<SeatUnit> findByIdWithLock(@Param("id") Long id);
}
