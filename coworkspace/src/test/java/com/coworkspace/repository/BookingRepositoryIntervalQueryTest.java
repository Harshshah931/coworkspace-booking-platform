package com.coworkspace.repository;

import com.coworkspace.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class BookingRepositoryIntervalQueryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookingRepository bookingRepository;

    private Space testSpace;
    private SeatUnit testSeatUnit;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .name("Test User")
                .email("testuser@example.com")
                .password("secret123")
                .role(Role.USER)
                .build();
        entityManager.persist(testUser);

        testSpace = Space.builder()
                .name("Downtown Hub")
                .location("Sector 1")
                .type(SpaceType.DESK)
                .pricePerDay(new BigDecimal("100.00"))
                .pricePerMonth(new BigDecimal("2000.00"))
                .build();
        entityManager.persist(testSpace);

        testSeatUnit = SeatUnit.builder()
                .label("Desk-A1")
                .type(SpaceType.DESK)
                .space(testSpace)
                .build();
        entityManager.persist(testSeatUnit);

        // Seed an existing confirmed booking from 2026-06-10 to 2026-06-15
        Booking existingBooking = Booking.builder()
                .user(testUser)
                .seatUnit(testSeatUnit)
                .startDate(LocalDate.of(2026, 6, 10))
                .endDate(LocalDate.of(2026, 6, 15))
                .bookingType(BookingType.DAILY)
                .status(BookingStatus.CONFIRMED)
                .totalPrice(new BigDecimal("500.00"))
                .build();
        entityManager.persist(existingBooking);
        entityManager.flush();
    }

    @Test
    @DisplayName("Should detect overlap when requested interval is enclosed within existing booking")
    void shouldDetectOverlap_EnclosedInterval() {
        boolean overlapping = bookingRepository.existsOverlappingBooking(
                testSeatUnit.getId(),
                LocalDate.of(2026, 6, 11),
                LocalDate.of(2026, 6, 14),
                BookingStatus.CANCELLED
        );
        assertTrue(overlapping, "Expected overlap for enclosed dates [June 11, June 14) inside [June 10, June 15)");
    }

    @Test
    @DisplayName("Should detect overlap when requested interval starts before and ends inside existing booking")
    void shouldDetectOverlap_OverlappingStart() {
        boolean overlapping = bookingRepository.existsOverlappingBooking(
                testSeatUnit.getId(),
                LocalDate.of(2026, 6, 8),
                LocalDate.of(2026, 6, 12),
                BookingStatus.CANCELLED
        );
        assertTrue(overlapping, "Expected overlap for dates [June 8, June 12) intersecting [June 10, June 15)");
    }

    @Test
    @DisplayName("Should detect overlap when requested interval starts inside and ends after existing booking")
    void shouldDetectOverlap_OverlappingEnd() {
        boolean overlapping = bookingRepository.existsOverlappingBooking(
                testSeatUnit.getId(),
                LocalDate.of(2026, 6, 13),
                LocalDate.of(2026, 6, 18),
                BookingStatus.CANCELLED
        );
        assertTrue(overlapping, "Expected overlap for dates [June 13, June 18) intersecting [June 10, June 15)");
    }

    @Test
    @DisplayName("Should detect overlap when requested interval completely encloses existing booking")
    void shouldDetectOverlap_CompletelyEnclosing() {
        boolean overlapping = bookingRepository.existsOverlappingBooking(
                testSeatUnit.getId(),
                LocalDate.of(2026, 6, 5),
                LocalDate.of(2026, 6, 20),
                BookingStatus.CANCELLED
        );
        assertTrue(overlapping, "Expected overlap for dates [June 5, June 20) enfolding [June 10, June 15)");
    }

    @Test
    @DisplayName("Should ALLOW adjacent interval where requested end date equals existing start date")
    void shouldAllowAdjacent_BeforeExistingBooking() {
        // [June 5, June 10) ends exactly on June 10 where the existing booking begins
        boolean overlapping = bookingRepository.existsOverlappingBooking(
                testSeatUnit.getId(),
                LocalDate.of(2026, 6, 5),
                LocalDate.of(2026, 6, 10),
                BookingStatus.CANCELLED
        );
        assertFalse(overlapping, "Adjacent interval ending on check-in day [June 5, June 10) must NOT overlap with [June 10, June 15)");
    }

    @Test
    @DisplayName("Should ALLOW adjacent interval where requested start date equals existing end date")
    void shouldAllowAdjacent_AfterExistingBooking() {
        // [June 15, June 20) starts exactly on June 15 where the existing booking ends
        boolean overlapping = bookingRepository.existsOverlappingBooking(
                testSeatUnit.getId(),
                LocalDate.of(2026, 6, 15),
                LocalDate.of(2026, 6, 20),
                BookingStatus.CANCELLED
        );
        assertFalse(overlapping, "Adjacent interval starting on check-out day [June 15, June 20) must NOT overlap with [June 10, June 15)");
    }

    @Test
    @DisplayName("Should ignore CANCELLED bookings during overlap check")
    void shouldIgnoreCancelledBookings() {
        // Create another seat unit with only a CANCELLED booking
        SeatUnit seat2 = SeatUnit.builder()
                .label("Desk-A2")
                .type(SpaceType.DESK)
                .space(testSpace)
                .build();
        entityManager.persist(seat2);

        Booking cancelledBooking = Booking.builder()
                .user(testUser)
                .seatUnit(seat2)
                .startDate(LocalDate.of(2026, 6, 10))
                .endDate(LocalDate.of(2026, 6, 15))
                .bookingType(BookingType.DAILY)
                .status(BookingStatus.CANCELLED)
                .totalPrice(new BigDecimal("500.00"))
                .build();
        entityManager.persist(cancelledBooking);
        entityManager.flush();

        boolean overlapping = bookingRepository.existsOverlappingBooking(
                seat2.getId(),
                LocalDate.of(2026, 6, 10),
                LocalDate.of(2026, 6, 15),
                BookingStatus.CANCELLED
        );
        assertFalse(overlapping, "Cancelled bookings must not block reservations on the same dates");
    }

    @Test
    @DisplayName("findBookedSeatUnitIds returns only booked seat unit IDs for the given space and range")
    void testFindBookedSeatUnitIds() {
        List<Long> booked = bookingRepository.findBookedSeatUnitIds(
                testSpace.getId(),
                LocalDate.of(2026, 6, 12),
                LocalDate.of(2026, 6, 13),
                BookingStatus.CANCELLED
        );
        assertEquals(1, booked.size());
        assertEquals(testSeatUnit.getId(), booked.get(0));

        List<Long> outsideRange = bookingRepository.findBookedSeatUnitIds(
                testSpace.getId(),
                LocalDate.of(2026, 6, 20),
                LocalDate.of(2026, 6, 25),
                BookingStatus.CANCELLED
        );
        assertTrue(outsideRange.isEmpty());
    }
}
