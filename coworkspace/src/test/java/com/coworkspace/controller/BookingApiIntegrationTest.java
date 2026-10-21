package com.coworkspace.controller;

import com.coworkspace.dto.BookingDtos.BookingRequest;
import com.coworkspace.entity.*;
import com.coworkspace.repository.BookingRepository;
import com.coworkspace.repository.SeatUnitRepository;
import com.coworkspace.repository.SpaceRepository;
import com.coworkspace.repository.UserRepository;
import com.coworkspace.security.JwtUtil;
import com.coworkspace.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:booking_api_it;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@AutoConfigureMockMvc
class BookingApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private SpaceRepository spaceRepository;
    @Autowired private SeatUnitRepository seatUnitRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    private String token;
    private Long seatUnitId;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        seatUnitRepository.deleteAll();
        spaceRepository.deleteAll();
        userRepository.deleteAll();

        User user = userRepository.save(User.builder()
                .name("Reservation Tester")
                .email("reservation@example.com")
                .password(passwordEncoder.encode("Password123"))
                .role(Role.USER)
                .build());
        token = jwtUtil.generateToken(new UserPrincipal(user));

        Space space = spaceRepository.save(Space.builder()
                .name("Integration Test Workspace")
                .location("Mumbai")
                .type(SpaceType.DESK)
                .description("Temporary test workspace")
                .pricePerDay(new BigDecimal("400.00"))
                .pricePerMonth(new BigDecimal("9000.00"))
                .build());
        SeatUnit unit = seatUnitRepository.save(SeatUnit.builder()
                .label("Desk-Test-1")
                .type(SpaceType.DESK)
                .space(space)
                .build());
        seatUnitId = unit.getId();
    }

    @Test
    void authenticatedCustomerCanCreateBookingDirectly() throws Exception {
        BookingRequest request = new BookingRequest(seatUnitId,
                LocalDate.now().plusDays(2), LocalDate.now().plusDays(5), BookingType.DAILY);

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.spaceName").value("Integration Test Workspace"))
                .andExpect(jsonPath("$.seatUnitLabel").value("Desk-Test-1"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalPrice").value(1200.0));
    }

    @Test
    void bookingApiDoesNotAcceptMissingDates() throws Exception {
        String request = "{\"seatUnitId\":" + seatUnitId + ",\"startDate\":null,\"endDate\":null,\"bookingType\":\"DAILY\"}";
        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }
}
