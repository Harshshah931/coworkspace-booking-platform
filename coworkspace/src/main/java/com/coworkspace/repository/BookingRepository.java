package com.coworkspace.repository;

import com.coworkspace.entity.Booking;
import com.coworkspace.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    /**
     * Core availability query.
     * A seat unit is UNAVAILABLE for [start, end) if there exists a
     * non-cancelled booking on it where:
     *   existing.startDate < requestedEnd  AND  existing.endDate > requestedStart
     * (classic interval-overlap condition)
     *
     * We use this to find which seat units, out of a given space, are
     * already booked for a given date range -> anything NOT in this list
     * is available.
     */
    @Query("""
            SELECT b.seatUnit.id FROM Booking b
            WHERE b.seatUnit.space.id = :spaceId
              AND b.status <> :cancelledStatus
              AND b.startDate < :requestedEnd
              AND b.endDate > :requestedStart
            """)
    List<Long> findBookedSeatUnitIds(
            @Param("spaceId") Long spaceId,
            @Param("requestedStart") LocalDate requestedStart,
            @Param("requestedEnd") LocalDate requestedEnd,
            @Param("cancelledStatus") BookingStatus cancelledStatus
    );

    /**
     * Checks if a specific seat unit has any overlapping non-cancelled booking.
     * Used at booking-creation time to prevent double-booking (race safety net
     * in addition to the availability check done beforehand).
     */
    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b
            WHERE b.seatUnit.id = :seatUnitId
              AND b.status <> :cancelledStatus
              AND b.startDate < :requestedEnd
              AND b.endDate > :requestedStart
            """)
    boolean existsOverlappingBooking(
            @Param("seatUnitId") Long seatUnitId,
            @Param("requestedStart") LocalDate requestedStart,
            @Param("requestedEnd") LocalDate requestedEnd,
            @Param("cancelledStatus") BookingStatus cancelledStatus
    );
}
