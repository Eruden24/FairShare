package io.github.eruden24.fairshare.repository;

import io.github.eruden24.fairshare.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface GroupRepository extends JpaRepository<Group, UUID> {

    @Query("select m.group from UserGroup m where m.user.id = :userId")
    List<Group> findAllByMemberId(@Param("userId") UUID userId);
}