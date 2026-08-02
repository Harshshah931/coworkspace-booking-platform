package com.coworkspace.service;

import com.coworkspace.dto.BookingDtos.BookingRequest;
import com.coworkspace.dto.BookingDtos.BookingResponse;
import com.coworkspace.entity.*;
import com.coworkspace.exception.BadRequestException;
import com.coworkspace.exception.ResourceNotFoundException;
import com.coworkspace.repository.BookingRepository;
import com.coworkspace.repository.SeatUnitRepository;
import com.coworkspace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SeatUnitRepository seatUnitRepository;
    private final UserRepository userRepository;

    public BookingResponse createBooking(String userEmail, BookingRequest request) {
        if (!request.getStartDate().isBefore(request.getEndDate())) {
            throw new BadRequestException("startDate must be before endDate");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SeatUnit seatUnit = seatUnitRepository.findById(request.getSeatUnitId())
                .orElseThrow(() -> new ResourceNotFoundException("Seat unit not found"));

        // Race-safety check: re-verify no overlapping booking exists right before insert.
        boolean overlapping = bookingRepository.existsOverlappingBooking(
                seatUnit.getId(), request.getStartDate(), request.getEndDate(), BookingStatus.CANCELLED
        );
        if (overlapping) {
            throw new BadRequestException("This seat is already booked for the selected dates");
        }

        BigDecimal totalPrice = calculatePrice(seatUnit, request);

        Booking booking = Booking.builder()
                .user(user)
                .seatUnit(seatUnit)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .bookingType(request.getBookingType())
                .status(BookingStatus.CONFIRMED)
                .totalPrice(totalPrice)
                .build();

        booking = bookingRepository.save(booking);
        return toResponse(booking);
    }

    public List<BookingResponse> getMyBookings(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return bookingRepository.findByUserId(user.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public BookingResponse cancelBooking(String userEmail, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getUser().getEmail().equals(userEmail)) {
            throw new BadRequestException("You can only cancel your own bookings");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking is already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking = bookingRepository.save(booking);
        return toResponse(booking);
    }

    private BigDecimal calculatePrice(SeatUnit seatUnit, BookingRequest request) {
        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());

        if (request.getBookingType() == BookingType.MONTHLY) {
            long months = Math.max(1, Math.round(days / 30.0));
            return seatUnit.getEffectivePricePerMonth().multiply(BigDecimal.valueOf(months));
        } else {
            long chargedDays = Math.max(1, days);
            return seatUnit.getEffectivePricePerDay().multiply(BigDecimal.valueOf(chargedDays));
        }
    }

    private BookingResponse toResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .spaceName(booking.getSeatUnit().getSpace().getName())
                .seatUnitLabel(booking.getSeatUnit().getLabel())
                .startDate(booking.getStartDate())
                .endDate(booking.getEndDate())
                .bookingType(booking.getBookingType())
                .status(booking.getStatus())
                .totalPrice(booking.getTotalPrice())
                .userEmail(booking.getUser().getEmail())
                .build();
    }
}
