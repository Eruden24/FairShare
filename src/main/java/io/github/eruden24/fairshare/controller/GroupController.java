package io.github.eruden24.fairshare.controller;

import io.github.eruden24.fairshare.config.CurrentUserId;
import io.github.eruden24.fairshare.dto.group.AddMemberRequest;
import io.github.eruden24.fairshare.dto.group.CreateGroupRequest;
import io.github.eruden24.fairshare.dto.group.GroupResponse;
import io.github.eruden24.fairshare.dto.group.MemberResponse;
import io.github.eruden24.fairshare.dto.group.TransferOwnershipRequest;
import io.github.eruden24.fairshare.dto.group.UpdateGroupRequest;
import io.github.eruden24.fairshare.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Groups", description = "Groups and their members. Non-members get 404; members get 403 on owner-only actions")
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @Operation(summary = "Create a group (you become its owner)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse create(@CurrentUserId UUID currentUserId,
                                @Valid @RequestBody CreateGroupRequest request) {
        return groupService.create(currentUserId, request);
    }

    @Operation(summary = "Groups I'm a member of")
    @GetMapping
    public List<GroupResponse> listMine(@CurrentUserId UUID currentUserId) {
        return groupService.listMine(currentUserId);
    }

    @Operation(summary = "Group details")
    @GetMapping("/{groupId}")
    public GroupResponse get(@CurrentUserId UUID currentUserId,
                             @PathVariable UUID groupId) {
        return groupService.get(currentUserId, groupId);
    }

    @Operation(summary = "Rename the group (owner)")
    @PatchMapping("/{groupId}")
    public GroupResponse rename(@CurrentUserId UUID currentUserId,
                                @PathVariable UUID groupId,
                                @Valid @RequestBody UpdateGroupRequest request) {
        return groupService.rename(currentUserId, groupId, request);
    }

    @Operation(summary = "Delete the group (owner, only when all balances are settled)")
    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUserId UUID currentUserId,
                       @PathVariable UUID groupId) {
        groupService.delete(currentUserId, groupId);
    }

    @Operation(summary = "List members")
    @GetMapping("/{groupId}/members")
    public List<MemberResponse> listMembers(@CurrentUserId UUID currentUserId,
                                            @PathVariable UUID groupId) {
        return groupService.listMembers(currentUserId, groupId);
    }

    @Operation(summary = "Add a member by email (owner)")
    @PostMapping("/{groupId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse addMember(@CurrentUserId UUID currentUserId,
                                    @PathVariable UUID groupId,
                                    @Valid @RequestBody AddMemberRequest request) {
        return groupService.addMember(currentUserId, groupId, request);
    }

    @Operation(summary = "Leave the group (not the owner, only with a zero balance)")
    @DeleteMapping("/{groupId}/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@CurrentUserId UUID currentUserId,
                      @PathVariable UUID groupId) {
        groupService.leave(currentUserId, groupId);
    }

    @Operation(summary = "Remove a member (owner, only when their balance is zero)")
    @DeleteMapping("/{groupId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@CurrentUserId UUID currentUserId,
                             @PathVariable UUID groupId,
                             @PathVariable UUID userId) {
        groupService.removeMember(currentUserId, groupId, userId);
    }

    @Operation(summary = "Make another member the owner (owner)")
    @PutMapping("/{groupId}/owner")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void transferOwnership(@CurrentUserId UUID currentUserId,
                                  @PathVariable UUID groupId,
                                  @Valid @RequestBody TransferOwnershipRequest request) {
        groupService.transferOwnership(currentUserId, groupId, request);
    }
}
