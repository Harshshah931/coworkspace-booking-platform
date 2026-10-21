package com.coworkspace.config;

import com.coworkspace.entity.*;
import com.coworkspace.repository.SeatUnitRepository;
import com.coworkspace.repository.SpaceRepository;
import com.coworkspace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final SpaceRepository spaceRepository;
    private final SeatUnitRepository seatUnitRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.seed.email:}")
    private String adminEmail;

    @Value("${admin.seed.password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        // Seed admin safely without printing credentials
        if (adminEmail != null && !adminEmail.isBlank() && adminPassword != null && !adminPassword.isBlank()) {
            if (!userRepository.existsByEmail(adminEmail)) {
                User admin = User.builder()
                        .name("Admin")
                        .email(adminEmail)
                        .password(passwordEncoder.encode(adminPassword))
                        .role(Role.ADMIN)
                        .build();
                userRepository.save(admin);
                log.info("Default administrator account created for email: {}", adminEmail);
            }
        }

        // Seed a sample space with seat units
        if (spaceRepository.count() == 0) {
            Space space = Space.builder()
                    .name("Innov8 Hub - Bandra")
                    .location("Bandra West, Mumbai")
                    .type(SpaceType.DESK)
                    .description("A vibrant coworking space with high-speed wifi and free coffee.")
                    .pricePerDay(new BigDecimal("500"))      // default desk price
                    .pricePerMonth(new BigDecimal("9000"))
                    .build();
            space = spaceRepository.save(space);

            // Desks use the space's default price (no override needed)
            for (int i = 1; i <= 8; i++) {
                SeatUnit desk = SeatUnit.builder()
                        .label("Desk-" + i)
                        .type(SpaceType.DESK)
                        .space(space)
                        .build();
                seatUnitRepository.save(desk);
            }

            // Cabins cost more — set an explicit per-seat override price
            for (int i = 1; i <= 3; i++) {
                SeatUnit cabin = SeatUnit.builder()
                        .label("Cabin-" + i)
                        .type(SpaceType.CABIN)
                        .pricePerDay(new BigDecimal("1200"))
                        .pricePerMonth(new BigDecimal("20000"))
                        .space(space)
                        .build();
                seatUnitRepository.save(cabin);
            }

            log.info("Initialized sample space: {} with 8 desks and 3 cabins", space.getName());
        }
    }
}
