package io.github.eruden24.fairshare.service;

import io.github.eruden24.fairshare.dto.group.CreateGroupRequest;
import io.github.eruden24.fairshare.dto.group.GroupResponse;
import io.github.eruden24.fairshare.entity.User;
import io.github.eruden24.fairshare.exception.NotFoundException;
import io.github.eruden24.fairshare.mapper.GroupMapper;
import io.github.eruden24.fairshare.repository.GroupRepository;
import io.github.eruden24.fairshare.repository.MemberRepository;
import io.github.eruden24.fairshare.repository.UserRepository;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final GroupMapper groupMapper;

    @Transactional
    public GroupResponse create(UUID currentUserId, CreateGroupRequest request) {
        User user = findUser(currentUserId);



        return null;
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("User not found:" + userId)
        );
    }


}
