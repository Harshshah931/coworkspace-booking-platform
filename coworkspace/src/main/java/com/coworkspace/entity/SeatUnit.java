package com.coworkspace.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    @JsonIgnore
    private Space space;
}
