package io.github.eruden24.fairshare.controller;

import io.github.eruden24.fairshare.config.CurrentUserId;
import io.github.eruden24.fairshare.dto.user.ChangePasswordRequest;
import io.github.eruden24.fairshare.dto.user.CreateUserRequest;
import io.github.eruden24.fairshare.dto.user.UpdateUserRequest;
import io.github.eruden24.fairshare.dto.user.UserResponse;
import io.github.eruden24.fairshare.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Users", description = "Registration and your own profile")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Register a new user")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody CreateUserRequest request) {
        return userService.register(request);
    }

    @Operation(summary = "My profile")
    @GetMapping("/me")
    public UserResponse me(@CurrentUserId UUID currentUserId) {
        return userService.getById(currentUserId);
    }

    @Operation(summary = "Update my display name and/or default currency")
    @PatchMapping("/me")
    public UserResponse updateMe(@CurrentUserId UUID currentUserId,
                                 @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateProfile(currentUserId, request);
    }

    @Operation(summary = "Change my password")
    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@CurrentUserId UUID currentUserId,
                               @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(currentUserId, request);
    }

    @Operation(summary = "Find a user by exact email")
    @GetMapping("/search")
    public UserResponse findByEmail(@RequestParam String email) {
        return userService.getByEmail(email);
    }
}
