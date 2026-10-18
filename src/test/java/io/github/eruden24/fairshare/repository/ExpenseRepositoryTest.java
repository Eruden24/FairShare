package io.github.eruden24.fairshare.repository;

import io.github.eruden24.fairshare.TestcontainersConfiguration;
import io.github.eruden24.fairshare.entity.Expense;
import io.github.eruden24.fairshare.entity.ExpenseSplit;
import io.github.eruden24.fairshare.entity.Group;
import io.github.eruden24.fairshare.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ExpenseRepositoryTest {

    @Autowired
    UserRepository userRepository;

    @Autowired
    GroupRepository groupRepository;

    @Autowired
    ExpenseRepository expenseRepository;

    @Autowired
    EntityManager entityManager;

    User anna;
    User bob;
    Group group;

    @BeforeEach
    void setUp() {
        anna = userRepository.save(new User("anna@example.com", "hash", "Anna"));
        bob = userRepository.save(new User("bob@example.com", "hash", "Bob"));
        group = groupRepository.save(new Group("Flat", "EUR", anna));
    }

    @Test
    void savesExpenseWithSplitsAndFindsItInItsGroup() {
        Expense expense = new Expense(group, "Groceries", 1000, anna, LocalDate.of(2026, 10, 1), anna);
        expense.addSplit(anna, 500);
        expense.addSplit(bob, 500);
        expenseRepository.saveAndFlush(expense);
        entityManager.clear();

        Expense found = expenseRepository.findByIdAndGroupId(expense.getId(), group.getId()).orElseThrow();

        assertThat(found.getAmountMinor()).isEqualTo(1000);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getSplits())
                .extracting(split -> split.getUser().getDisplayName(), ExpenseSplit::getAmountMinor)
                .containsExactlyInAnyOrder(
                        tuple("Anna", 500L),
                        tuple("Bob", 500L));
    }

    @Test
    void doesNotFindExpenseThroughAnotherGroup() {
        Group otherGroup = groupRepository.save(new Group("Other", "EUR", anna));
        Expense expense = new Expense(group, "Groceries", 1000, anna, LocalDate.now(), anna);
        expense.addSplit(anna, 1000);
        expenseRepository.saveAndFlush(expense);

        assertThat(expenseRepository.findByIdAndGroupId(expense.getId(), otherGroup.getId())).isEmpty();
    }

    @Test
    void rejectsNonPositiveAmount() {
        Expense expense = new Expense(group, "Nothing", 0, anna, LocalDate.now(), anna);

        assertThatThrownBy(() -> expenseRepository.saveAndFlush(expense))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsTwoSplitsForTheSameUser() {
        Expense expense = new Expense(group, "Dinner", 1000, anna, LocalDate.now(), anna);
        expense.addSplit(bob, 500);
        expense.addSplit(bob, 500);

        assertThatThrownBy(() -> expenseRepository.saveAndFlush(expense))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
