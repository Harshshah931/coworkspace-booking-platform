package com.coworkspace.service;

import com.coworkspace.dto.BookingDtos.BookingRequest;
import com.coworkspace.dto.BookingDtos.BookingResponse;
import com.coworkspace.entity.*;
import com.coworkspace.exception.BadRequestException;
import com.coworkspace.repository.BookingRepository;
import com.coworkspace.repository.SeatUnitRepository;
import com.coworkspace.repository.SpaceRepository;
import com.coworkspace.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BookingConcurrencyIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private SeatUnitRepository seatUnitRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long testSeatUnitId;
    private final List<String> testUserEmails = new ArrayList<>();

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        seatUnitRepository.deleteAll();
        spaceRepository.deleteAll();
        userRepository.deleteAll();

        // Create 10 distinct users for concurrent booking simulation
        for (int i = 1; i <= 10; i++) {
            String email = "concurrent_user_" + i + "@example.com";
            User user = User.builder()
                    .name("User " + i)
                    .email(email)
                    .password(passwordEncoder.encode("password123"))
                    .role(Role.USER)
                    .build();
            userRepository.save(user);
            testUserEmails.add(email);
        }

        // Create a single Space with a single SeatUnit
        Space space = Space.builder()
                .name("Concurrency Test Hub")
                .location("Downtown")
                .type(SpaceType.DESK)
                .pricePerDay(new BigDecimal("250.00"))
                .pricePerMonth(new BigDecimal("5000.00"))
                .build();
        space = spaceRepository.save(space);

        SeatUnit seatUnit = SeatUnit.builder()
                .label("Contested-Desk-1")
                .type(SpaceType.DESK)
                .space(space)
                .build();
        seatUnit = seatUnitRepository.save(seatUnit);
        testSeatUnitId = seatUnit.getId();
    }

    @Test
    @DisplayName("Simultaneous concurrent requests to reserve the same seat unit must result in exactly 1 booking")
    void testConcurrentBookingAttemptsOnSameSeat() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<String> errors = Collections.synchronizedList(new ArrayList<>());

        LocalDate start = LocalDate.of(2026, 7, 1);
        LocalDate end = LocalDate.of(2026, 7, 5);

        for (int i = 0; i < threadCount; i++) {
            final String userEmail = testUserEmails.get(i);
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    // All threads wait here until startLatch is released, ensuring simultaneous arrival
                    startLatch.await();
                    BookingRequest request = new BookingRequest(
                            testSeatUnitId,
                            start,
                            end,
                            BookingType.DAILY
                    );
                    BookingResponse response = bookingService.createBooking(userEmail, request);
                    if (response != null && response.getId() != null) {
                        successCount.incrementAndGet();
                    }
                } catch (BadRequestException ex) {
                    failureCount.incrementAndGet();
                    errors.add(ex.getMessage());
                } catch (Exception ex) {
                    errors.add("Unexpected error: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                }
            });
        }

        // Wait until all threads are initialized and ready
        readyLatch.await(5, TimeUnit.SECONDS);
        // Release all threads simultaneously
        startLatch.countDown();

        executor.shutdown();
        boolean finished = executor.awaitTermination(15, TimeUnit.SECONDS);
        assertTrue(finished, "Executor did not finish within 15 seconds");

        // Verify concurrency protection invariants:
        // 1. Exactly 1 request must have succeeded
        assertEquals(1, successCount.get(), "Exactly one concurrent booking should succeed");

        // 2. The other 9 requests must have failed with double-booking prevention message
        assertEquals(threadCount - 1, failureCount.get(), "All other concurrent attempts must be rejected");
        for (String error : errors) {
            assertEquals("This seat is already booked for the selected dates", error);
        }

        // 3. Database consistency: Exactly 1 record persisted
        List<Booking> persistedBookings = bookingRepository.findAll();
        assertEquals(1, persistedBookings.size(), "Database must contain exactly 1 booking record");
        assertEquals(BookingStatus.CONFIRMED, persistedBookings.get(0).getStatus());
        assertEquals(testSeatUnitId, persistedBookings.get(0).getSeatUnit().getId());
    }
}
