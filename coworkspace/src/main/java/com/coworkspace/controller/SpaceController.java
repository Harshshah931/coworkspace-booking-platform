package com.coworkspace.controller;

import com.coworkspace.dto.SpaceDtos.*;
import com.coworkspace.service.SpaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/spaces")
@RequiredArgsConstructor
public class SpaceController {

    private final SpaceService spaceService;

    // ---- Public browse endpoints ----

    @GetMapping
    public ResponseEntity<List<SpaceResponse>> getAllSpaces() {
        return ResponseEntity.ok(spaceService.getAllSpaces());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpaceResponse> getSpace(@PathVariable Long id) {
        return ResponseEntity.ok(spaceService.getSpaceById(id));
    }

    // Core realtime seat-map availability endpoint
    @GetMapping("/{id}/availability")
    public ResponseEntity<AvailabilityResponse> getAvailability(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(spaceService.getAvailability(id, start, end));
    }

    // ---- Admin-only management endpoints ----

    @PostMapping
    public ResponseEntity<SpaceResponse> createSpace(@Valid @RequestBody SpaceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(spaceService.createSpace(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SpaceResponse> updateSpace(@PathVariable Long id, @Valid @RequestBody SpaceRequest request) {
        return ResponseEntity.ok(spaceService.updateSpace(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSpace(@PathVariable Long id) {
        spaceService.deleteSpace(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/seat-units")
    public ResponseEntity<SeatUnitResponse> addSeatUnit(@PathVariable Long id, @Valid @RequestBody SeatUnitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(spaceService.addSeatUnit(id, request));
    }
}
