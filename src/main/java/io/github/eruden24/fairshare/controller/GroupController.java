package io.github.eruden24.fairshare.controller;

import io.github.eruden24.fairshare.config.CurrentUserId;
import io.github.eruden24.fairshare.dto.group.AddMemberRequest;
import io.github.eruden24.fairshare.dto.group.CreateGroupRequest;
import io.github.eruden24.fairshare.dto.group.GroupResponse;
import io.github.eruden24.fairshare.dto.group.MemberResponse;
import io.github.eruden24.fairshare.dto.group.TransferOwnershipRequest;
import io.github.eruden24.fairshare.dto.group.UpdateGroupRequest;
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

    private final GroupService groupService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse create(@CurrentUserId UUID currentUserId,
                                @Valid @RequestBody CreateGroupRequest request) {
        return groupService.create(currentUserId, request);
    }

    @GetMapping
    public List<GroupResponse> listMine(@CurrentUserId UUID currentUserId) {
        return groupService.listMine(currentUserId);
    }

    @GetMapping("/{groupId}")
    public GroupResponse get(@CurrentUserId UUID currentUserId,
                             @PathVariable UUID groupId) {
        return groupService.get(currentUserId, groupId);
    }

    @PatchMapping("/{groupId}")
    public GroupResponse rename(@CurrentUserId UUID currentUserId,
                                @PathVariable UUID groupId,
                                @Valid @RequestBody UpdateGroupRequest request) {
        return groupService.rename(currentUserId, groupId, request);
    }

    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUserId UUID currentUserId,
                       @PathVariable UUID groupId) {
        groupService.delete(currentUserId, groupId);
    }

    @GetMapping("/{groupId}/members")
    public List<MemberResponse> listMembers(@CurrentUserId UUID currentUserId,
                                            @PathVariable UUID groupId) {
        return groupService.listMembers(currentUserId, groupId);
    }

    @PostMapping("/{groupId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse addMember(@CurrentUserId UUID currentUserId,
                                    @PathVariable UUID groupId,
                                    @Valid @RequestBody AddMemberRequest request) {
        return groupService.addMember(currentUserId, groupId, request);
    }

    @DeleteMapping("/{groupId}/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@CurrentUserId UUID currentUserId,
                      @PathVariable UUID groupId) {
        groupService.leave(currentUserId, groupId);
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@CurrentUserId UUID currentUserId,
                             @PathVariable UUID groupId,
                             @PathVariable UUID userId) {
        groupService.removeMember(currentUserId, groupId, userId);
    }

    @PutMapping("/{groupId}/owner")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void transferOwnership(@CurrentUserId UUID currentUserId,
                                  @PathVariable UUID groupId,
                                  @Valid @RequestBody TransferOwnershipRequest request) {
        groupService.transferOwnership(currentUserId, groupId, request);
    }
}
