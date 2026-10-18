package io.github.eruden24.fairshare.service;

import io.github.eruden24.fairshare.dto.user.ChangePasswordRequest;
import io.github.eruden24.fairshare.dto.user.CreateUserRequest;
import io.github.eruden24.fairshare.dto.user.UpdateUserRequest;
import io.github.eruden24.fairshare.dto.user.UserResponse;
import io.github.eruden24.fairshare.entity.User;
import io.github.eruden24.fairshare.exception.ConflictException;
import io.github.eruden24.fairshare.exception.ForbiddenException;
import io.github.eruden24.fairshare.mapper.UserMapper;
import io.github.eruden24.fairshare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, Mappers.getMapper(UserMapper.class));
    }

    @Test
    void registerNormalizesEmailAndHashesPassword() {
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.register(
                new CreateUserRequest("  Alice@Example.COM ", "password123", " Alice ", "USD")
        );

        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.displayName()).isEqualTo("Alice");
        assertThat(response.defaultCurrency()).isEqualTo("USD");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(
                new CreateUserRequest("alice@example.com", "password123", "Alice", null)
        )).isInstanceOf(ConflictException.class);

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateProfileOnlyChangesProvidedFields() {
        UUID id = UUID.randomUUID();
        User user = new User("alice@example.com", "hashed", "Alice");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        UserResponse response = userService.updateProfile(id, new UpdateUserRequest(null, "GBP"));

        assertThat(response.displayName()).isEqualTo("Alice");
        assertThat(response.defaultCurrency()).isEqualTo("GBP");
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        UUID id = UUID.randomUUID();
        User user = new User("alice@example.com", "hashed", "Alice");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(id, new ChangePasswordRequest("wrong", "newPassword1")))
                .isInstanceOf(ForbiddenException.class);

        assertThat(user.getPasswordHash()).isEqualTo("hashed");
    }

    @Test
    void changePasswordStoresNewHash() {
        UUID id = UUID.randomUUID();
        User user = new User("alice@example.com", "hashed", "Alice");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(passwordEncoder.encode("newPassword1")).thenReturn("new-hash");

        userService.changePassword(id, new ChangePasswordRequest("password123", "newPassword1"));

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
    }
}
