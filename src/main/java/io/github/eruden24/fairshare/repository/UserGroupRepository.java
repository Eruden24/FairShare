package io.github.eruden24.fairshare.repository;

import io.github.eruden24.fairshare.entity.UserGroup;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserGroupRepository extends JpaRepository<UserGroup, UUID> {

    @EntityGraph(attributePaths = "user")
    List<UserGroup> findByGroupId(UUID groupId);
    Optional<UserGroup> findByGroupIdAndUserId(UUID groupId, UUID userId);
    boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);
}
