package io.github.eruden24.fairshare.service;

import io.github.eruden24.fairshare.dto.user.ChangePasswordRequest;
import io.github.eruden24.fairshare.dto.user.CreateUserRequest;
import io.github.eruden24.fairshare.dto.user.UpdateUserRequest;
import io.github.eruden24.fairshare.dto.user.UserResponse;
import io.github.eruden24.fairshare.entity.User;
import io.github.eruden24.fairshare.exception.ConflictException;
import io.github.eruden24.fairshare.exception.ForbiddenException;
import io.github.eruden24.fairshare.exception.NotFoundException;
import io.github.eruden24.fairshare.mapper.UserMapper;
import io.github.eruden24.fairshare.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse register(CreateUserRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email is already registered");
        }

        User user = new User(
                email,
                passwordEncoder.encode(request.password()),
                request.displayName().trim()
        );

        if (request.defaultCurrency() != null) {
            user.setDefaultCurrency(request.defaultCurrency());
        }

        return userMapper.toResponse(userRepository.saveAndFlush(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return userMapper.toResponse(findUser(id));
    }

    @Transactional(readOnly = true)
    public UserResponse getByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email)).map(userMapper::toResponse).orElseThrow(
                () -> new NotFoundException("No user registered with email: " + email)
        );
    }

    @Transactional
    public UserResponse updateProfile(UUID id, UpdateUserRequest request) {
        User user = findUser(id);

        if (request.displayName() != null) {
            user.setDisplayName(request.displayName().trim());
        }
        if (request.defaultCurrency() != null) {
            user.setDefaultCurrency(request.defaultCurrency());
        }

        return userMapper.toResponse(userRepository.saveAndFlush(user));
    }

    @Transactional
    public void changePassword(UUID id, ChangePasswordRequest request) {
        User user = findUser(id);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ForbiddenException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private User findUser(UUID id) {
        return userRepository.findById(id).orElseThrow(
                () -> new NotFoundException("User not found: " + id)
        );
    }
}
