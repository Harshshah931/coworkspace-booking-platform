package com.coworkspace.service;

import com.coworkspace.dto.ProfileDtos.*;
import com.coworkspace.entity.User;
import com.coworkspace.exception.BadRequestException;
import com.coworkspace.exception.ResourceNotFoundException;
import com.coworkspace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileResponse getProfile(String email) {
        User user = getUserOrThrow(email);
        return toResponse(user);
    }

    public ProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = getUserOrThrow(email);
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user = userRepository.save(user);
        return toResponse(user);
    }

    public void changePassword(String email, ChangePasswordRequest request) {
        User user = getUserOrThrow(email);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private User getUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private ProfileResponse toResponse(User user) {
        return ProfileResponse.builder()
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .build();
    }
}
