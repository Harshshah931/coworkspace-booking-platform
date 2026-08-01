package com.coworkspace.repository;

import com.coworkspace.entity.SeatUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatUnitRepository extends JpaRepository<SeatUnit, Long> {
    List<SeatUnit> findBySpaceId(Long spaceId);
}
