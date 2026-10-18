package io.github.eruden24.fairshare.service;

import io.github.eruden24.fairshare.dto.group.AddMemberRequest;
import io.github.eruden24.fairshare.dto.group.CreateGroupRequest;
import io.github.eruden24.fairshare.dto.group.GroupResponse;
import io.github.eruden24.fairshare.dto.group.MemberResponse;
import io.github.eruden24.fairshare.dto.group.TransferOwnershipRequest;
import io.github.eruden24.fairshare.dto.group.UpdateGroupRequest;
import io.github.eruden24.fairshare.entity.Group;
import io.github.eruden24.fairshare.entity.UserGroup;
import io.github.eruden24.fairshare.enumeration.GroupRole;
import io.github.eruden24.fairshare.entity.User;
import io.github.eruden24.fairshare.exception.ConflictException;
import io.github.eruden24.fairshare.exception.ForbiddenException;
import io.github.eruden24.fairshare.exception.NotFoundException;
import io.github.eruden24.fairshare.mapper.GroupMapper;
import io.github.eruden24.fairshare.repository.GroupRepository;
import io.github.eruden24.fairshare.repository.UserGroupRepository;
import io.github.eruden24.fairshare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Access rules: a group is only visible to its members. For anyone else it does not exist (404),
 * so group ids can't be probed. Members who try an owner-only action get 403.
 */
@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserRepository userRepository;
    private final GroupMapper groupMapper;

    @Transactional
    public GroupResponse create(UUID currentUserId, CreateGroupRequest request) {
        User creator = userRepository.findById(currentUserId).orElseThrow(
                () -> new NotFoundException("User not found: " + currentUserId)
        );

        Group group = groupRepository.saveAndFlush(
                new Group(request.name().trim(), request.currency(), creator)
        );

        userGroupRepository.save(new UserGroup(group, creator, GroupRole.OWNER));

        return groupMapper.toResponse(group, 1, GroupRole.OWNER);
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> listMine(UUID currentUserId) {
        List<UserGroup> memberships = userGroupRepository.findByUserIdOrderByJoinedAtAsc(currentUserId);
        if (memberships.isEmpty()) {
            return List.of();
        }

        Map<UUID, Long> memberCounts = userGroupRepository
                .countMembersByGroupIds(memberships.stream().map(m -> m.getGroup().getId()).toList())
                .stream()
                .collect(Collectors.toMap(
                        UserGroupRepository.MemberCount::getGroupId,
                        UserGroupRepository.MemberCount::getMemberCount
                ));

        return memberships.stream()
                .map(m -> groupMapper.toResponse(m.getGroup(), memberCounts.get(m.getGroup().getId()), m.getRole()))
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupResponse get(UUID currentUserId, UUID groupId) {
        return toResponse(requireMember(groupId, currentUserId));
    }

    @Transactional
    public GroupResponse rename(UUID currentUserId, UUID groupId, UpdateGroupRequest request) {
        UserGroup owner = requireOwner(groupId, currentUserId, "rename the group");

        owner.getGroup().setName(request.name().trim());
        groupRepository.flush();

        return toResponse(owner);
    }

    @Transactional
    public void delete(UUID currentUserId, UUID groupId) {
        UserGroup owner = requireOwner(groupId, currentUserId, "delete the group");

        // Remove memberships through JPA first: the owner's membership is loaded in this session,
        // and Hibernate refuses to flush an entity that still points at a deleted group
        userGroupRepository.deleteAll(userGroupRepository.findByGroupIdOrderByJoinedAtAsc(groupId));
        groupRepository.delete(owner.getGroup());
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(UUID currentUserId, UUID groupId) {
        requireMember(groupId, currentUserId);
        return groupMapper.toMemberResponse(userGroupRepository.findByGroupIdOrderByJoinedAtAsc(groupId));
    }

    @Transactional
    public MemberResponse addMember(UUID currentUserId, UUID groupId, AddMemberRequest request) {
        UserGroup owner = requireOwner(groupId, currentUserId, "add members");

        User newMember = userRepository.findByEmailIgnoreCase(UserService.normalizeEmail(request.email())).orElseThrow(
                () -> new NotFoundException("No user registered with email: " + request.email())
        );

        if (userGroupRepository.existsByGroupIdAndUserId(groupId, newMember.getId())) {
            throw new ConflictException("User is already a member of this group");
        }

        UserGroup member = userGroupRepository.saveAndFlush(
                new UserGroup(owner.getGroup(), newMember, GroupRole.MEMBER)
        );

        return groupMapper.toMemberResponse(member);
    }

    @Transactional
    public void removeMember(UUID currentUserId, UUID groupId, UUID memberUserId) {
        requireOwner(groupId, currentUserId, "remove members");

        if (memberUserId.equals(currentUserId)) {
            throw new ConflictException("The owner cannot be removed; transfer ownership first");
        }

        userGroupRepository.delete(findMembership(groupId, memberUserId));
    }

    @Transactional
    public void leave(UUID currentUserId, UUID groupId) {
        UserGroup membership = requireMember(groupId, currentUserId);

        if (membership.getRole() == GroupRole.OWNER) {
            throw new ConflictException("The owner cannot leave the group; transfer ownership first");
        }

        userGroupRepository.delete(membership);
    }

    @Transactional
    public void transferOwnership(UUID currentUserId, UUID groupId, TransferOwnershipRequest request) {
        UserGroup currentOwner = requireOwner(groupId, currentUserId, "transfer ownership");

        if (request.userId().equals(currentUserId)) {
            throw new ConflictException("You are already the owner of this group");
        }

        UserGroup newOwner = findMembership(groupId, request.userId());

        // Demote first and flush: the database allows only one OWNER per group at any moment
        currentOwner.setRole(GroupRole.MEMBER);
        userGroupRepository.saveAndFlush(currentOwner);

        newOwner.setRole(GroupRole.OWNER);
        userGroupRepository.saveAndFlush(newOwner);
    }

    private GroupResponse toResponse(UserGroup membership) {
        Group group = membership.getGroup();
        return groupMapper.toResponse(group, userGroupRepository.countByGroupId(group.getId()), membership.getRole());
    }

    private UserGroup findMembership(UUID groupId, UUID userId) {
        return userGroupRepository.findByGroupIdAndUserId(groupId, userId).orElseThrow(
                () -> new NotFoundException("User is not a member of this group: " + userId)
        );
    }

    private UserGroup requireMember(UUID groupId, UUID userId) {
        return userGroupRepository.findByGroupIdAndUserId(groupId, userId).orElseThrow(
                () -> new NotFoundException("Group not found: " + groupId)
        );
    }

    private UserGroup requireOwner(UUID groupId, UUID userId, String action) {
        UserGroup membership = requireMember(groupId, userId);

        if (membership.getRole() != GroupRole.OWNER) {
            throw new ForbiddenException("Only the group owner can " + action);
        }

        return membership;
    }
}
