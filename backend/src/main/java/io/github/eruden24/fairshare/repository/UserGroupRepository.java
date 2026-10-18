package io.github.eruden24.fairshare.repository;

import io.github.eruden24.fairshare.entity.UserGroup;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserGroupRepository extends JpaRepository<UserGroup, UUID> {

    @EntityGraph(attributePaths = "user")
    List<UserGroup> findByGroupIdOrderByJoinedAtAsc(UUID groupId);

    @EntityGraph(attributePaths = "group")
    List<UserGroup> findByUserIdOrderByJoinedAtAsc(UUID userId);

    @EntityGraph(attributePaths = "group")
    Optional<UserGroup> findByGroupIdAndUserId(UUID groupId, UUID userId);

    boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);

    long countByGroupId(UUID groupId);

    @Query("""
            select ug.group.id as groupId, count(ug) as memberCount
            from UserGroup ug
            where ug.group.id in :groupIds
            group by ug.group.id
            """)
    List<MemberCount> countMembersByGroupIds(@Param("groupIds") Collection<UUID> groupIds);

    interface MemberCount {
        UUID getGroupId();
        long getMemberCount();
    }
}
