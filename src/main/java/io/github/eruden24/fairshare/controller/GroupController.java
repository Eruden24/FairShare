package io.github.eruden24.fairshare.controller;

import io.github.eruden24.fairshare.dto.group.CreateGroupRequest;
import io.github.eruden24.fairshare.dto.group.GroupResponse;
import io.github.eruden24.fairshare.repository.GroupRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupRepository groupRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse create(@RequestHeader("X-user-Id") UUID currentUserId,
            @Valid @RequestBody CreateGroupRequest request) {
        return groupRepository.create(currentUserId, request);
    }
}
