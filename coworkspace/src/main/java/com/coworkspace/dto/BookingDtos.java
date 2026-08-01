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
        @NotNull
        private Long seatUnitId;

        @NotNull
        private LocalDate startDate;

        @NotNull
        private LocalDate endDate;

        @NotNull
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
