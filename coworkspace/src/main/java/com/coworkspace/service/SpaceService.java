package com.coworkspace.service;

import com.coworkspace.dto.SpaceDtos.*;
import com.coworkspace.entity.BookingStatus;
import com.coworkspace.entity.SeatUnit;
import com.coworkspace.entity.Space;
import com.coworkspace.exception.BadRequestException;
import com.coworkspace.exception.ResourceNotFoundException;
import com.coworkspace.repository.BookingRepository;
import com.coworkspace.repository.SeatUnitRepository;
import com.coworkspace.repository.SpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpaceService {

    private final SpaceRepository spaceRepository;
    private final SeatUnitRepository seatUnitRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public SpaceResponse createSpace(SpaceRequest request) {
        Space space = Space.builder()
                .name(request.getName())
                .location(request.getLocation())
                .type(request.getType())
                .description(request.getDescription())
                .pricePerDay(request.getPricePerDay())
                .pricePerMonth(request.getPricePerMonth())
                .build();

        space = spaceRepository.save(space);
        return toResponse(space);
    }

    @Transactional(readOnly = true)
    public List<SpaceResponse> getAllSpaces() {
        return spaceRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SpaceResponse getSpaceById(Long id) {
        return toResponse(getSpaceOrThrow(id));
    }

    @Transactional
    public SpaceResponse updateSpace(Long id, SpaceRequest request) {
        Space space = getSpaceOrThrow(id);
        space.setName(request.getName());
        space.setLocation(request.getLocation());
        space.setType(request.getType());
        space.setDescription(request.getDescription());
        space.setPricePerDay(request.getPricePerDay());
        space.setPricePerMonth(request.getPricePerMonth());
        space = spaceRepository.save(space);
        return toResponse(space);
    }

    @Transactional
    public void deleteSpace(Long id) {
        Space space = getSpaceOrThrow(id);
        if (bookingRepository.existsBySeatUnit_Space_Id(id)) {
            throw new BadRequestException("This workspace has booking history and cannot be deleted. You can edit its details instead.");
        }
        spaceRepository.delete(space);
    }

    @Transactional
    public SeatUnitResponse addSeatUnit(Long spaceId, SeatUnitRequest request) {
        Space space = getSpaceOrThrow(spaceId);

        SeatUnit seatUnit = SeatUnit.builder()
                .label(request.getLabel())
                .type(request.getType())
                .pricePerDay(request.getPricePerDay())     // null is fine -> falls back to space price
                .pricePerMonth(request.getPricePerMonth())
                .space(space)
                .build();

        seatUnit = seatUnitRepository.save(seatUnit);

        return SeatUnitResponse.builder()
                .id(seatUnit.getId())
                .label(seatUnit.getLabel())
                .type(seatUnit.getType())
                .available(true)
                .pricePerDay(seatUnit.getEffectivePricePerDay())
                .pricePerMonth(seatUnit.getEffectivePricePerMonth())
                .build();
    }

    /**
     * The core "realtime seat-map availability" feature.
     * Returns every seat unit in the space, flagged available/unavailable
     * for the requested date range, along with each seat's effective price
     * (its own override price, or the space's default price).
     */
    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailability(Long spaceId, LocalDate start, LocalDate end) {
        if (!start.isBefore(end)) {
            throw new BadRequestException("startDate must be before endDate");
        }

        Space space = getSpaceOrThrow(spaceId);

        List<SeatUnit> allSeatUnits = seatUnitRepository.findBySpaceId(spaceId);

        Set<Long> bookedIds = bookingRepository
                .findBookedSeatUnitIds(spaceId, start, end, BookingStatus.CANCELLED)
                .stream()
                .collect(Collectors.toSet());

        List<SeatUnitResponse> seatUnitResponses = allSeatUnits.stream()
                .map(su -> SeatUnitResponse.builder()
                        .id(su.getId())
                        .label(su.getLabel())
                        .type(su.getType())
                        .available(!bookedIds.contains(su.getId()))
                        .pricePerDay(su.getEffectivePricePerDay())
                        .pricePerMonth(su.getEffectivePricePerMonth())
                        .build())
                .collect(Collectors.toList());

        return AvailabilityResponse.builder()
                .spaceId(spaceId)
                .startDate(start.toString())
                .endDate(end.toString())
                .seatUnits(seatUnitResponses)
                .build();
    }

    Space getSpaceOrThrow(Long id) {
        return spaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found with id: " + id));
    }

    private SpaceResponse toResponse(Space space) {
        return SpaceResponse.builder()
                .id(space.getId())
                .name(space.getName())
                .location(space.getLocation())
                .type(space.getType())
                .description(space.getDescription())
                .pricePerDay(space.getPricePerDay())
                .pricePerMonth(space.getPricePerMonth())
                .totalSeatUnits(space.getSeatUnits() != null ? space.getSeatUnits().size() : 0)
                .build();
    }
}
