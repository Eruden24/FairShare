package io.github.eruden24.fairshare.repository;

import io.github.eruden24.fairshare.entity.ExpenseGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ExpenseGroupRepository extends JpaRepository<ExpenseGroup, UUID> {

    @Query("select m.group from GroupMember m where m.user.id = :userId")
    List<ExpenseGroup> findAllByMemberId(@Param("userId") UUID userId);
}
