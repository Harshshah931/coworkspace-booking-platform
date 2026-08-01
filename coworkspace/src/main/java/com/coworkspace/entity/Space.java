package com.coworkspace.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "spaces")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Space {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpaceType type;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private BigDecimal pricePerDay;

    @Column(nullable = false)
    private BigDecimal pricePerMonth;

    @Builder.Default
    @OneToMany(mappedBy = "space", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SeatUnit> seatUnits = new ArrayList<>();
}
