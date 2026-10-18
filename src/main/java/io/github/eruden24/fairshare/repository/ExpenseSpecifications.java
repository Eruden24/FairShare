package io.github.eruden24.fairshare.repository;

import io.github.eruden24.fairshare.entity.Expense;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builds the WHERE clause for listing expenses. Each filter is only added when it's given,
 * which a single @Query with "(:param is null or ...)" can't do cleanly.
 */
public final class ExpenseSpecifications {

    private ExpenseSpecifications() {
    }

    public static Specification<Expense> matching(UUID groupId, LocalDate from, LocalDate to, UUID paidById) {
        List<Specification<Expense>> filters = new ArrayList<>();
        filters.add((root, query, cb) -> cb.equal(root.get("group").get("id"), groupId));
        if (from != null) {
            filters.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("expenseDate"), from));
        }
        if (to != null) {
            filters.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("expenseDate"), to));
        }
        if (paidById != null) {
            filters.add((root, query, cb) -> cb.equal(root.get("paidBy").get("id"), paidById));
        }
        return Specification.allOf(filters);
    }
}
