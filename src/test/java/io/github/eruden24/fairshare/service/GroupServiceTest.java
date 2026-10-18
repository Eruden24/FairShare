package io.github.eruden24.fairshare.service;

import io.github.eruden24.fairshare.dto.group.AddMemberRequest;
import io.github.eruden24.fairshare.dto.group.TransferOwnershipRequest;
import io.github.eruden24.fairshare.entity.Group;
import io.github.eruden24.fairshare.entity.User;
import io.github.eruden24.fairshare.entity.UserGroup;
import io.github.eruden24.fairshare.enumeration.GroupRole;
import io.github.eruden24.fairshare.exception.ConflictException;
import io.github.eruden24.fairshare.exception.ForbiddenException;
import io.github.eruden24.fairshare.exception.NotFoundException;
import io.github.eruden24.fairshare.mapper.GroupMapper;
import io.github.eruden24.fairshare.repository.GroupRepository;
import io.github.eruden24.fairshare.repository.UserGroupRepository;
import io.github.eruden24.fairshare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    GroupRepository groupRepository;

    @Mock
    UserGroupRepository userGroupRepository;

    @Mock
    UserRepository userRepository;

    GroupService groupService;

    final UUID groupId = UUID.randomUUID();
    final UUID ownerId = UUID.randomUUID();
    final UUID memberId = UUID.randomUUID();

    User owner;
    User member;
    Group group;

    @BeforeEach
    void setUp() {
        groupService = new GroupService(groupRepository, userGroupRepository, userRepository,
                Mappers.getMapper(GroupMapper.class), new GroupAccess(userGroupRepository));
        owner = new User("owner@example.com", "hash", "Owner");
        member = new User("member@example.com", "hash", "Member");
        group = new Group("Trip", "EUR", owner);
    }

    @Test
    void nonMemberGetsNotFound() {
        when(userGroupRepository.findByGroupIdAndUserId(groupId, memberId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.get(memberId, groupId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void memberCannotAddMembers() {
        givenMembership(memberId, member, GroupRole.MEMBER);

        assertThatThrownBy(() -> groupService.addMember(memberId, groupId, new AddMemberRequest("new@example.com")))
                .isInstanceOf(ForbiddenException.class);

        verify(userGroupRepository, never()).saveAndFlush(any());
    }

    @Test
    void addingExistingMemberIsConflict() {
        givenMembership(ownerId, owner, GroupRole.OWNER);
        when(userRepository.findByEmailIgnoreCase("member@example.com")).thenReturn(Optional.of(member));
        when(userGroupRepository.existsByGroupIdAndUserId(groupId, member.getId())).thenReturn(true);

        assertThatThrownBy(() -> groupService.addMember(ownerId, groupId, new AddMemberRequest("Member@Example.com")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void ownerCannotLeave() {
        givenMembership(ownerId, owner, GroupRole.OWNER);

        assertThatThrownBy(() -> groupService.leave(ownerId, groupId))
                .isInstanceOf(ConflictException.class);

        verify(userGroupRepository, never()).delete(any());
    }

    @Test
    void memberCanLeave() {
        UserGroup membership = givenMembership(memberId, member, GroupRole.MEMBER);

        groupService.leave(memberId, groupId);

        verify(userGroupRepository).delete(membership);
    }

    @Test
    void ownerCannotRemoveThemselves() {
        givenMembership(ownerId, owner, GroupRole.OWNER);

        assertThatThrownBy(() -> groupService.removeMember(ownerId, groupId, ownerId))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void transferOwnershipSwapsRoles() {
        UserGroup ownerMembership = givenMembership(ownerId, owner, GroupRole.OWNER);
        UserGroup memberMembership = givenMembership(memberId, member, GroupRole.MEMBER);

        groupService.transferOwnership(ownerId, groupId, new TransferOwnershipRequest(memberId));

        assertThat(ownerMembership.getRole()).isEqualTo(GroupRole.MEMBER);
        assertThat(memberMembership.getRole()).isEqualTo(GroupRole.OWNER);
    }

    @Test
    void transferOwnershipToNonMemberIsNotFound() {
        UserGroup ownerMembership = givenMembership(ownerId, owner, GroupRole.OWNER);
        when(userGroupRepository.findByGroupIdAndUserId(groupId, memberId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.transferOwnership(ownerId, groupId, new TransferOwnershipRequest(memberId)))
                .isInstanceOf(NotFoundException.class);

        assertThat(ownerMembership.getRole()).isEqualTo(GroupRole.OWNER);
    }

    private UserGroup givenMembership(UUID userId, User user, GroupRole role) {
        UserGroup membership = new UserGroup(group, user, role);
        when(userGroupRepository.findByGroupIdAndUserId(groupId, userId)).thenReturn(Optional.of(membership));
        return membership;
    }
}
