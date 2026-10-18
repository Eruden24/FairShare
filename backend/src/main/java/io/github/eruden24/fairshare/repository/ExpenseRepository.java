package io.github.eruden24.fairshare.repository;

import io.github.eruden24.fairshare.entity.Expense;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID>, JpaSpecificationExecutor<Expense> {

    // Matching on the group too, so an expense id from another group is simply "not found"
    @EntityGraph(attributePaths = {"group", "splits", "splits.user"})
    Optional<Expense> findByIdAndGroupId(UUID id, UUID groupId);

    // --- Totals for balances, summed by the database ---

    @Query("""
            select e.paidBy.id as userId, sum(e.amountMinor) as total
            from Expense e
            where e.group.id = :groupId
            group by e.paidBy.id
            """)
    List<UserTotal> sumPaidPerUser(@Param("groupId") UUID groupId);

    @Query("""
            select s.user.id as userId, sum(s.amountMinor) as total
            from ExpenseSplit s
            where s.expense.group.id = :groupId
            group by s.user.id
            """)
    List<UserTotal> sumOwedPerUser(@Param("groupId") UUID groupId);

    @Query("""
            select e.group.id as groupId, sum(e.amountMinor) as total
            from Expense e
            where e.paidBy.id = :userId
            group by e.group.id
            """)
    List<GroupTotal> sumPaidPerGroup(@Param("userId") UUID userId);

    @Query("""
            select s.expense.group.id as groupId, sum(s.amountMinor) as total
            from ExpenseSplit s
            where s.user.id = :userId
            group by s.expense.group.id
            """)
    List<GroupTotal> sumOwedPerGroup(@Param("userId") UUID userId);
}
