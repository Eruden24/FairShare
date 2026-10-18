package io.github.eruden24.fairshare.service;

import io.github.eruden24.fairshare.dto.group.AddMemberRequest;
import io.github.eruden24.fairshare.dto.group.CreateGroupRequest;
import io.github.eruden24.fairshare.dto.group.GroupResponse;
import io.github.eruden24.fairshare.dto.group.MemberResponse;
import io.github.eruden24.fairshare.entity.ExpenseGroup;
import io.github.eruden24.fairshare.entity.GroupMember;
import io.github.eruden24.fairshare.enumeration.GroupRole;
import io.github.eruden24.fairshare.entity.User;
import io.github.eruden24.fairshare.exception.ConflictException;
import io.github.eruden24.fairshare.exception.ForbiddenException;
import io.github.eruden24.fairshare.exception.NotFoundException;
import io.github.eruden24.fairshare.mapper.GroupMapper;
import io.github.eruden24.fairshare.mapper.UserMapper;
import io.github.eruden24.fairshare.repository.GroupRepository;
import io.github.eruden24.fairshare.repository.MemberRepository;
import io.github.eruden24.fairshare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final GroupMapper groupMapper;
    private final UserMapper userMapper;

    @Transactional
    public GroupResponse create(UUID currentUserId, CreateGroupRequest request) {
        User creator = findUser(currentUserId);

        ExpenseGroup group = groupRepository.saveAndFlush(
                new ExpenseGroup(request.name(), request.currency(), creator)
        );

        memberRepository.save(new GroupMember(group, creator, GroupRole.OWNER));

        return groupMapper.toResponse(group);
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> listMine(UUID currentUserId) {
        return groupMapper.toResponse(
                groupRepository.findAllByMemberId(currentUserId)
        );
    }

    @Transactional(readOnly = true)
    public GroupResponse get(UUID currentUserId, UUID groupId) {
        ExpenseGroup group = findGroup(groupId);
        requireMember(groupId, currentUserId);

        return groupMapper.toResponse(group);
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(UUID currentUserId, UUID groupId) {
        findGroup(groupId);
        requireMember(groupId, currentUserId);
        return groupMapper.toMemberResponse(memberRepository.findByGroupId(groupId));
    }
    
    @Transactional
    public MemberResponse addMember(UUID currentUserId, UUID groupId, AddMemberRequest request) {

        ExpenseGroup group = findGroup(groupId);
        
        GroupMember caller = requireMember(groupId, currentUserId);
        
        if (caller.getRole() != GroupRole.OWNER) {
            throw new ForbiddenException("Only the group owner can add member");
        }
        
        User newMember = findUser(request.userId());

        if (memberRepository.existsByGroupIdAndUserId(groupId, newMember.getId())) {
            throw new ConflictException("User is already a member of this group");
        }
        
        GroupMember member = memberRepository.saveAndFlush(
                new GroupMember(group, newMember, GroupRole.MEMBER)
        );

        return groupMapper.toMemberResponse(member);
    }

    private ExpenseGroup findGroup(UUID groupId) {
        return groupRepository.findById(groupId).orElseThrow(
                () -> new NotFoundException("Group not found: " + groupId)
        );
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("User not found:" + userId)
        );
    }

    private GroupMember requireMember(UUID groupId, UUID userId) {
        return  memberRepository.findByGroupIdAndUserId(groupId, userId).orElseThrow(
                () -> new ForbiddenException("You are not a member of this group")
        );
    }


}
