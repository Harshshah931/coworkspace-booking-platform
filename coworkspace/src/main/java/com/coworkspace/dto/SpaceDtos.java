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

        // Base/default price for this space. Individual seat units (e.g. cabins)
        // can override this with their own price — see SeatUnitRequest.
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

        // Optional. If omitted, this seat unit uses the parent space's price.
        // Typically set for CABIN units to charge more than DESK units.
        private BigDecimal pricePerDay;
        private BigDecimal pricePerMonth;
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
        private BigDecimal pricePerDay;
        private BigDecimal pricePerMonth;
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
