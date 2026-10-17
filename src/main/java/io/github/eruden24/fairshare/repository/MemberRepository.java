package io.github.eruden24.fairshare.repository;

import io.github.eruden24.fairshare.entity.GroupMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<GroupMember, UUID> {

    @EntityGraph(attributePaths = "user")
    List<GroupMember> findByGroupId(UUID groupId);
    Optional<GroupMember> findByGroupIdAndUserId(UUID groupId, UUID userId);
    boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);
}
