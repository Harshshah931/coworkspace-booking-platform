package com.coworkspace.dto;

import com.coworkspace.entity.BookingStatus;
import com.coworkspace.entity.BookingType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BookingDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingRequest {
        @NotNull(message = "Choose a workspace unit")
        private Long seatUnitId;

        @NotNull(message = "Choose a start date")
        private LocalDate startDate;

        @NotNull(message = "Choose an end date")
        private LocalDate endDate;

        @NotNull(message = "Choose a booking type")
        private BookingType bookingType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookingResponse {
        private Long id;
        private String spaceName;
        private String seatUnitLabel;
        private LocalDate startDate;
        private LocalDate endDate;
        private BookingType bookingType;
        private BookingStatus status;
        private BigDecimal totalPrice;
        private String userEmail;
    }
}
