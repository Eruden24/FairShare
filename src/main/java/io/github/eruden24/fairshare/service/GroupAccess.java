package io.github.eruden24.fairshare.service;

import io.github.eruden24.fairshare.entity.UserGroup;
import io.github.eruden24.fairshare.enumeration.GroupRole;
import io.github.eruden24.fairshare.exception.ForbiddenException;
import io.github.eruden24.fairshare.exception.NotFoundException;
import io.github.eruden24.fairshare.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Access rules shared by everything inside a group: a group is only visible to its members.
 * For anyone else it does not exist (404), so group ids can't be probed. Members who try an
 * owner-only action get 403.
 */
@Component
@RequiredArgsConstructor
public class GroupAccess {

    private final UserGroupRepository userGroupRepository;

    public UserGroup requireMember(UUID groupId, UUID userId) {
        return userGroupRepository.findByGroupIdAndUserId(groupId, userId).orElseThrow(
                () -> new NotFoundException("Group not found: " + groupId)
        );
    }

    public UserGroup requireOwner(UUID groupId, UUID userId, String action) {
        UserGroup membership = requireMember(groupId, userId);

        if (membership.getRole() != GroupRole.OWNER) {
            throw new ForbiddenException("Only the group owner can " + action);
        }

        return membership;
    }
}
