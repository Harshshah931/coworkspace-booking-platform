package com.coworkspace.config;

import com.coworkspace.entity.*;
import com.coworkspace.repository.SeatUnitRepository;
import com.coworkspace.repository.SpaceRepository;
import com.coworkspace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final SpaceRepository spaceRepository;
    private final SeatUnitRepository seatUnitRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Seed admin
        if (!userRepository.existsByEmail("admin@coworkspace.com")) {
            User admin = User.builder()
                    .name("Admin")
                    .email("admin@coworkspace.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);
            System.out.println(">>> Seeded admin: admin@coworkspace.com / admin123");
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

            System.out.println(">>> Seeded sample space: " + space.getName() + " with 8 desks ($500/day) + 3 cabins ($1200/day)");
        }
    }
}
