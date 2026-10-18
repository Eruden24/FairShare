package io.github.eruden24.fairshare.controller;

import io.github.eruden24.fairshare.config.CurrentUserId;
import io.github.eruden24.fairshare.dto.user.ChangePasswordRequest;
import io.github.eruden24.fairshare.dto.user.CreateUserRequest;
import io.github.eruden24.fairshare.dto.user.UpdateUserRequest;
import io.github.eruden24.fairshare.dto.user.UserResponse;
import io.github.eruden24.fairshare.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody CreateUserRequest request) {
        return userService.register(request);
    }

    @GetMapping("/me")
    public UserResponse me(@CurrentUserId UUID currentUserId) {
        return userService.getById(currentUserId);
    }

    @PatchMapping("/me")
    public UserResponse updateMe(@CurrentUserId UUID currentUserId,
                                 @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateProfile(currentUserId, request);
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@CurrentUserId UUID currentUserId,
                               @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(currentUserId, request);
    }

    @GetMapping("/search")
    public UserResponse findByEmail(@RequestParam String email) {
        return userService.getByEmail(email);
    }
}
