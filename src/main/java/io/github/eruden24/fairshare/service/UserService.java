package io.github.eruden24.fairshare.service;

import io.github.eruden24.fairshare.dto.user.CreateUserRequest;
import io.github.eruden24.fairshare.dto.user.UserResponse;
import io.github.eruden24.fairshare.entity.User;
import io.github.eruden24.fairshare.exception.ConflictException;
import io.github.eruden24.fairshare.exception.NotFoundException;
import io.github.eruden24.fairshare.mapper.UserMapper;
import io.github.eruden24.fairshare.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse register(CreateUserRequest request) {
        String email = request.email().trim();

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
        return userRepository.findById(id).map(userMapper::toResponse).orElseThrow(
                () -> new NotFoundException("User not found: " + id)
        );
    }
}
