package io.github.eruden24.fairshare.repository;

import io.github.eruden24.fairshare.TestcontainersConfiguration;
import io.github.eruden24.fairshare.entity.Expense;
import io.github.eruden24.fairshare.entity.ExpenseSplit;
import io.github.eruden24.fairshare.entity.Group;
import io.github.eruden24.fairshare.entity.User;
import io.github.eruden24.fairshare.enumeration.SplitType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
        Expense expense = expense("Groceries", 1000, anna, LocalDate.of(2026, 10, 1));
        expense.replaceSplits(List.of(share(anna, 500), share(bob, 500)));
        expenseRepository.saveAndFlush(expense);
        entityManager.clear();

        Expense found = expenseRepository.findByIdAndGroupId(expense.getId(), group.getId()).orElseThrow();

        assertThat(found.getAmountMinor()).isEqualTo(1000);
        assertThat(found.getSplitType()).isEqualTo(SplitType.EQUAL);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getSplits())
                .extracting(split -> split.getUser().getDisplayName(), ExpenseSplit::getAmountMinor)
                .containsExactlyInAnyOrder(tuple("Anna", 500L), tuple("Bob", 500L));
    }

    @Test
    void doesNotFindExpenseThroughAnotherGroup() {
        Group otherGroup = groupRepository.save(new Group("Other", "EUR", anna));
        Expense expense = expense("Groceries", 1000, anna, LocalDate.now());
        expense.replaceSplits(List.of(share(anna, 1000)));
        expenseRepository.saveAndFlush(expense);

        assertThat(expenseRepository.findByIdAndGroupId(expense.getId(), otherGroup.getId())).isEmpty();
    }

    @Test
    void replacingSplitsUpdatesRowsInPlace() {
        Expense expense = expense("Dinner", 1000, anna, LocalDate.now());
        expense.replaceSplits(List.of(share(anna, 500), share(bob, 500)));
        expenseRepository.saveAndFlush(expense);

        // Same people again (would break the unique constraint if rows were deleted and re-inserted), then Bob alone
        expense.replaceSplits(List.of(share(anna, 700), share(bob, 300)));
        expenseRepository.saveAndFlush(expense);
        expense.replaceSplits(List.of(new Expense.Share(bob, 1000, new BigDecimal("2"))));
        expenseRepository.saveAndFlush(expense);
        entityManager.clear();

        Expense found = expenseRepository.findByIdAndGroupId(expense.getId(), group.getId()).orElseThrow();
        assertThat(found.getSplits())
                .extracting(split -> split.getUser().getDisplayName(), ExpenseSplit::getAmountMinor)
                .containsExactly(tuple("Bob", 1000L));
        assertThat(found.getSplits().getFirst().getValue()).isEqualByComparingTo("2");
    }

    @Test
    void filtersAndPagesNewestFirst() {
        saveExpense("Old", 100, anna, LocalDate.of(2026, 1, 10));
        saveExpense("Middle", 200, bob, LocalDate.of(2026, 2, 10));
        saveExpense("New", 300, anna, LocalDate.of(2026, 3, 10));
        Sort newestFirst = Sort.by(Sort.Order.desc("expenseDate"));

        Page<Expense> firstPage = expenseRepository.findAll(
                ExpenseSpecifications.matching(group.getId(), null, null, null), PageRequest.of(0, 2, newestFirst));
        assertThat(firstPage.getContent()).extracting(Expense::getDescription).containsExactly("New", "Middle");
        assertThat(firstPage.getTotalElements()).isEqualTo(3);

        Page<Expense> filtered = expenseRepository.findAll(
                ExpenseSpecifications.matching(group.getId(), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 31), anna.getId()),
                PageRequest.of(0, 10, newestFirst));
        assertThat(filtered.getContent()).extracting(Expense::getDescription).containsExactly("New");
    }

    @Test
    void sumsPaidAndOwedPerUserAndPerGroup() {
        // Anna pays 30 for both, Bob pays 10 for both
        Expense dinner = expense("Dinner", 3000, anna, LocalDate.now());
        dinner.replaceSplits(List.of(share(anna, 1500), share(bob, 1500)));
        expenseRepository.save(dinner);
        Expense taxi = expense("Taxi", 1000, bob, LocalDate.now());
        taxi.replaceSplits(List.of(share(anna, 500), share(bob, 500)));
        expenseRepository.saveAndFlush(taxi);

        assertThat(expenseRepository.sumPaidPerUser(group.getId()))
                .extracting(ExpenseRepository.UserTotal::getUserId, ExpenseRepository.UserTotal::getTotal)
                .containsExactlyInAnyOrder(tuple(anna.getId(), 3000L), tuple(bob.getId(), 1000L));
        assertThat(expenseRepository.sumOwedPerUser(group.getId()))
                .extracting(ExpenseRepository.UserTotal::getUserId, ExpenseRepository.UserTotal::getTotal)
                .containsExactlyInAnyOrder(tuple(anna.getId(), 2000L), tuple(bob.getId(), 2000L));

        assertThat(expenseRepository.sumPaidPerGroup(anna.getId()))
                .extracting(ExpenseRepository.GroupTotal::getGroupId, ExpenseRepository.GroupTotal::getTotal)
                .containsExactly(tuple(group.getId(), 3000L));
        assertThat(expenseRepository.sumOwedPerGroup(anna.getId()))
                .extracting(ExpenseRepository.GroupTotal::getGroupId, ExpenseRepository.GroupTotal::getTotal)
                .containsExactly(tuple(group.getId(), 2000L));
    }

    @Test
    void rejectsNonPositiveAmount() {
        Expense expense = expense("Nothing", 0, anna, LocalDate.now());

        assertThatThrownBy(() -> expenseRepository.saveAndFlush(expense))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Expense expense(String description, long amountMinor, User payer, LocalDate date) {
        return new Expense(group, description, amountMinor, payer, date, SplitType.EQUAL, anna);
    }

    private void saveExpense(String description, long amountMinor, User payer, LocalDate date) {
        Expense expense = expense(description, amountMinor, payer, date);
        expense.replaceSplits(List.of(share(payer, amountMinor)));
        expenseRepository.saveAndFlush(expense);
    }

    private static Expense.Share share(User user, long amountMinor) {
        return new Expense.Share(user, amountMinor, null);
    }
}
