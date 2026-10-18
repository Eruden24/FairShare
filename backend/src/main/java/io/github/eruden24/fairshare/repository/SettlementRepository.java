package io.github.eruden24.fairshare.repository;

import io.github.eruden24.fairshare.entity.Settlement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SettlementRepository extends JpaRepository<Settlement, UUID> {

    @EntityGraph(attributePaths = {"group", "fromUser", "toUser"})
    List<Settlement> findByGroupIdOrderBySettledOnDescCreatedAtDesc(UUID groupId);

    @EntityGraph(attributePaths = {"group", "fromUser", "toUser"})
    Optional<Settlement> findByIdAndGroupId(UUID id, UUID groupId);

    // --- Totals for balances, summed by the database ---

    @Query("""
            select s.fromUser.id as userId, sum(s.amountMinor) as total
            from Settlement s
            where s.group.id = :groupId
            group by s.fromUser.id
            """)
    List<UserTotal> sumSentPerUser(@Param("groupId") UUID groupId);

    @Query("""
            select s.toUser.id as userId, sum(s.amountMinor) as total
            from Settlement s
            where s.group.id = :groupId
            group by s.toUser.id
            """)
    List<UserTotal> sumReceivedPerUser(@Param("groupId") UUID groupId);

    @Query("""
            select s.group.id as groupId, sum(s.amountMinor) as total
            from Settlement s
            where s.fromUser.id = :userId
            group by s.group.id
            """)
    List<GroupTotal> sumSentPerGroup(@Param("userId") UUID userId);

    @Query("""
            select s.group.id as groupId, sum(s.amountMinor) as total
            from Settlement s
            where s.toUser.id = :userId
            group by s.group.id
            """)
    List<GroupTotal> sumReceivedPerGroup(@Param("userId") UUID userId);
}
