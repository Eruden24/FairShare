package io.github.eruden24.fairshare.repository;

import io.github.eruden24.fairshare.entity.Expense;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    // Matching on the group too, so an expense id from another group is simply "not found"
    @EntityGraph(attributePaths = {"group", "splits", "splits.user"})
    Optional<Expense> findByIdAndGroupId(UUID id, UUID groupId);
}
