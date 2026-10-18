package io.github.eruden24.fairshare.controller;

import io.github.eruden24.fairshare.dto.group.AddMemberRequest;
import io.github.eruden24.fairshare.dto.group.CreateGroupRequest;
import io.github.eruden24.fairshare.dto.group.GroupResponse;
import io.github.eruden24.fairshare.dto.group.MemberResponse;
import io.github.eruden24.fairshare.repository.GroupRepository;
import io.github.eruden24.fairshare.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {
    private static final String USER_HEADER = "X-User-Id";

    private final GroupService groupService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse create(@RequestHeader(USER_HEADER) UUID currentUserId,
    @Valid @RequestBody CreateGroupRequest request) {
        return groupService.create(currentUserId, request);
    }

    @GetMapping
    public List<GroupResponse> listMine(@RequestHeader(USER_HEADER) UUID currentUserId) {
        return groupService.listMine(currentUserId);
    }

    @GetMapping("/{groupId}")
    public GroupResponse get(@RequestHeader(USER_HEADER) UUID currentUserId,
                             @PathVariable UUID groupId) {
        return groupService.get(currentUserId, groupId);
    }

    @GetMapping("/{groupId}/members")
    public List<MemberResponse> listMembers(@RequestHeader(USER_HEADER) UUID currentUserId,
                                            @PathVariable UUID groupId) {
        return groupService.listMembers(currentUserId, groupId);
    }

    @PostMapping("/{groupId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse addMember(@RequestHeader(USER_HEADER) UUID currentUserId,
                                    @PathVariable UUID groupId,
                                    @Valid @RequestBody AddMemberRequest request) {
        return groupService.addMember(currentUserId, groupId, request);
    }
}
