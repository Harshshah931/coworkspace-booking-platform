package com.coworkspace.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "seat_units")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // e.g. "Desk-12", "Cabin-3"
    @Column(nullable = false)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpaceType type;

    // Optional per-seat override. If null, price falls back to the parent Space's price.
    // This lets cabins cost more than desks within the same space.
    @Column(name = "price_per_day")
    private BigDecimal pricePerDay;

    @Column(name = "price_per_month")
    private BigDecimal pricePerMonth;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    @JsonIgnore
    private Space space;

    /**
     * Effective daily price: this seat's own price if set, otherwise the parent space's price.
     */
    @Transient
    public BigDecimal getEffectivePricePerDay() {
        return pricePerDay != null ? pricePerDay : (space != null ? space.getPricePerDay() : null);
    }

    @Transient
    public BigDecimal getEffectivePricePerMonth() {
        return pricePerMonth != null ? pricePerMonth : (space != null ? space.getPricePerMonth() : null);
    }
}
