package io.github.eruden24.fairshare.controller;

import io.github.eruden24.fairshare.dto.auth.LoginRequest;
import io.github.eruden24.fairshare.dto.auth.TokenResponse;
import io.github.eruden24.fairshare.mapper.GroupMapper;
import io.github.eruden24.fairshare.repository.GroupRepository;
import io.github.eruden24.fairshare.repository.UserGroupRepository;
import io.github.eruden24.fairshare.service.AuthService;
import io.github.eruden24.fairshare.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.loggit add src/main/java/io/github/eruden24/fairshare/controller/GroupController.java \
        src/main/java/io/github/eruden24/fairshare/service/ GroupService.java \
        src/main/java/io/github/eruden24/fairshare/dto/group/ \
        src/main/java/io/github/eruden24/fairshare/mapper/ GroupMapper.java \
        src/main/java/io/github/eruden24/fairshare/repository/ GroupRepository.java \
        src/main/java/io/github/eruden24/fairshare/repository/ UserGroupRepository.java \
        src/main/resources/db/migration/V4__one_owner_per_group.sql \
        src/test/java/io/github/eruden24/fairshare/repository/GroupRepositoryTest.java \
        src/test/java/io/github/eruden24/fairshare/service/GroupServiceTest.java \
        src/test/java/io/github/eruden24/fairshare/controller/
                git commit -m "Add group rename/delete, leave, remove member, transfer ownership and one-owner rule"in(request);
    }
}
