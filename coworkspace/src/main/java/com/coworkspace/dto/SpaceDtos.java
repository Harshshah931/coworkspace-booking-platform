package com.coworkspace.dto;

import com.coworkspace.entity.SpaceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public class SpaceDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpaceRequest {
        @NotBlank
        private String name;

        @NotBlank
        private String location;

        @NotNull
        private SpaceType type;

        private String description;

        @NotNull @Positive
        private BigDecimal pricePerDay;

        @NotNull @Positive
        private BigDecimal pricePerMonth;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.Builder
    public static class SpaceResponse {
        private Long id;
        private String name;
        private String location;
        private SpaceType type;
        private String description;
        private BigDecimal pricePerDay;
        private BigDecimal pricePerMonth;
        private int totalSeatUnits;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatUnitRequest {
        @NotBlank
        private String label;

        @NotNull
        private SpaceType type;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.Builder
    public static class SeatUnitResponse {
        private Long id;
        private String label;
        private SpaceType type;
        private boolean available;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.Builder
    public static class AvailabilityResponse {
        private Long spaceId;
        private String startDate;
        private String endDate;
        private List<SeatUnitResponse> seatUnits;
    }
}
