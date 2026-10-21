package com.coworkspace.service;

import com.coworkspace.dto.ProfileDtos.*;
import com.coworkspace.entity.User;
import com.coworkspace.exception.BadRequestException;
import com.coworkspace.exception.ResourceNotFoundException;
import com.coworkspace.repository.UserRepository;
import com.coworkspace.util.PhoneNumberUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileResponse getProfile(String email) {
        return toResponse(getUserOrThrow(email));
    }

    @Transactional
    public ProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = getUserOrThrow(email);
        user.setName(request.getName().trim());
        String newPhone = request.getPhone() == null || request.getPhone().isBlank()
                ? null : PhoneNumberUtil.normalizeAndValidate(request.getPhone());
        if (!java.util.Objects.equals(newPhone, user.getPhone())) {
            assertPhoneAvailable(newPhone, user);
            user.setPhone(newPhone);
            user.setPhoneVerified(false); // Legacy column only; no verification flow depends on it.
        }
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = getUserOrThrow(email);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private void assertPhoneAvailable(String phone, User user) {
        if (phone == null) return;
        userRepository.findByPhone(phone)
                .filter(other -> !other.getId().equals(user.getId()))
                .ifPresent(other -> { throw new BadRequestException("This phone number is already used by another account"); });
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
