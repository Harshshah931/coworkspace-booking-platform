package com.coworkspace.service;

import com.coworkspace.dto.BookingDtos.BookingRequest;
import com.coworkspace.dto.BookingDtos.BookingResponse;
import com.coworkspace.entity.*;
import com.coworkspace.exception.BadRequestException;
import com.coworkspace.exception.ResourceNotFoundException;
import com.coworkspace.repository.BookingRepository;
import com.coworkspace.repository.SeatUnitRepository;
import com.coworkspace.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SeatUnitRepository seatUnitRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingService bookingService;

    private User sampleUser;
    private Space sampleSpace;
    private SeatUnit sampleSeatUnit;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .name("Alice")
                .email("alice@example.com")
                .password("encoded_pass")
                .role(Role.USER)
                .build();

        sampleSpace = Space.builder()
                .id(10L)
                .name("Innov8 Hub")
                .location("Mumbai")
                .type(SpaceType.DESK)
                .pricePerDay(new BigDecimal("500.00"))
                .pricePerMonth(new BigDecimal("9000.00"))
                .build();

        sampleSeatUnit = SeatUnit.builder()
                .id(100L)
                .label("Desk-1")
                .type(SpaceType.DESK)
                .space(sampleSpace)
                .build();
    }

    @Test
    @DisplayName("Should create booking when seat is available and lock is acquired")
    void createBooking_Success() {
        BookingRequest request = new BookingRequest(
                100L,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 5),
                BookingType.DAILY
        );

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser));
        when(seatUnitRepository.findByIdWithLock(100L)).thenReturn(Optional.of(sampleSeatUnit));
        when(bookingRepository.existsOverlappingBooking(eq(100L), any(), any(), eq(BookingStatus.CANCELLED)))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(500L);
            return b;
        });

        BookingResponse response = bookingService.createBooking("alice@example.com", request);

        assertNotNull(response);
        assertEquals(500L, response.getId());
        assertEquals("Innov8 Hub", response.getSpaceName());
        assertEquals("Desk-1", response.getSeatUnitLabel());
        assertEquals(BookingStatus.CONFIRMED, response.getStatus());
        // 4 days * $500 = $2000
        assertEquals(new BigDecimal("2000.00"), response.getTotalPrice());
        verify(seatUnitRepository).findByIdWithLock(100L);
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should reject booking when startDate is not before endDate")
    void createBooking_Fails_WhenStartDateNotBeforeEndDate() {
        BookingRequest invalidSameDay = new BookingRequest(
                100L,
                LocalDate.of(2026, 6, 5),
                LocalDate.of(2026, 6, 5),
                BookingType.DAILY
        );

        BadRequestException ex1 = assertThrows(BadRequestException.class,
                () -> bookingService.createBooking("alice@example.com", invalidSameDay));
        assertEquals("startDate must be before endDate", ex1.getMessage());

        BookingRequest invalidReversed = new BookingRequest(
                100L,
                LocalDate.of(2026, 6, 6),
                LocalDate.of(2026, 6, 5),
                BookingType.DAILY
        );

        BadRequestException ex2 = assertThrows(BadRequestException.class,
                () -> bookingService.createBooking("alice@example.com", invalidReversed));
        assertEquals("startDate must be before endDate", ex2.getMessage());
    }

    @Test
    @DisplayName("Should reject booking when seat is already booked for overlapping dates")
    void createBooking_Fails_WhenOverlappingBookingExists() {
        BookingRequest request = new BookingRequest(
                100L,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 5),
                BookingType.DAILY
        );

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser));
        when(seatUnitRepository.findByIdWithLock(100L)).thenReturn(Optional.of(sampleSeatUnit));
        when(bookingRepository.existsOverlappingBooking(eq(100L), any(), any(), eq(BookingStatus.CANCELLED)))
                .thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> bookingService.createBooking("alice@example.com", request));
        assertEquals("This seat is already booked for the selected dates", ex.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should calculate monthly price correctly based on 30-day rounding")
    void createBooking_CalculatesMonthlyPrice() {
        BookingRequest request = new BookingRequest(
                100L,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 7, 1),
                BookingType.MONTHLY
        );

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser));
        when(seatUnitRepository.findByIdWithLock(100L)).thenReturn(Optional.of(sampleSeatUnit));
        when(bookingRepository.existsOverlappingBooking(eq(100L), any(), any(), eq(BookingStatus.CANCELLED)))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.createBooking("alice@example.com", request);

        // 30 days = 1 month * $9000 = $9000
        assertEquals(new BigDecimal("9000.00"), response.getTotalPrice());
    }

    @Test
    @DisplayName("Should allow user to cancel their own confirmed booking")
    void cancelBooking_Success() {
        Booking booking = Booking.builder()
                .id(200L)
                .user(sampleUser)
                .seatUnit(sampleSeatUnit)
                .status(BookingStatus.CONFIRMED)
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 5))
                .totalPrice(new BigDecimal("2000.00"))
                .bookingType(BookingType.DAILY)
                .build();

        when(bookingRepository.findById(200L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.cancelBooking("alice@example.com", 200L);

        assertEquals(BookingStatus.CANCELLED, response.getStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    @DisplayName("Should prevent user from cancelling someone else's booking")
    void cancelBooking_Fails_WhenNotOwner() {
        User otherUser = User.builder().id(2L).email("bob@example.com").build();
        Booking booking = Booking.builder()
                .id(200L)
                .user(otherUser)
                .status(BookingStatus.CONFIRMED)
                .build();

        when(bookingRepository.findById(200L)).thenReturn(Optional.of(booking));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> bookingService.cancelBooking("alice@example.com", 200L));
        assertEquals("You can only cancel your own bookings", ex.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject cancelling an already cancelled booking")
    void cancelBooking_Fails_WhenAlreadyCancelled() {
        Booking booking = Booking.builder()
                .id(200L)
                .user(sampleUser)
                .status(BookingStatus.CANCELLED)
                .build();

        when(bookingRepository.findById(200L)).thenReturn(Optional.of(booking));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> bookingService.cancelBooking("alice@example.com", 200L));
        assertEquals("Booking is already cancelled", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when booking does not exist")
    void cancelBooking_Fails_WhenNotFound() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.cancelBooking("alice@example.com", 999L));
    }
}
