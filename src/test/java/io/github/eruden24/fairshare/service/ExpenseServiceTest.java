package io.github.eruden24.fairshare.service;

import io.github.eruden24.fairshare.dto.expense.ExpenseRequest;
import io.github.eruden24.fairshare.dto.expense.ExpenseResponse;
import io.github.eruden24.fairshare.dto.expense.ParticipantRequest;
import io.github.eruden24.fairshare.dto.expense.SplitResponse;
import io.github.eruden24.fairshare.entity.Expense;
import io.github.eruden24.fairshare.entity.Group;
import io.github.eruden24.fairshare.entity.User;
import io.github.eruden24.fairshare.entity.UserGroup;
import io.github.eruden24.fairshare.enumeration.GroupRole;
import io.github.eruden24.fairshare.enumeration.SplitType;
import io.github.eruden24.fairshare.exception.ForbiddenException;
import io.github.eruden24.fairshare.exception.InvalidRequestException;
import io.github.eruden24.fairshare.exception.NotFoundException;
import io.github.eruden24.fairshare.mapper.ExpenseMapper;
import io.github.eruden24.fairshare.repository.ExpenseRepository;
import io.github.eruden24.fairshare.repository.UserGroupRepository;
import io.github.eruden24.fairshare.split.SplitCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    ExpenseRepository expenseRepository;

    @Mock
    UserGroupRepository userGroupRepository;

    ExpenseService expenseService;

    final UUID groupId = UUID.randomUUID();
    final UUID expenseId = UUID.randomUUID();

    User anna;
    User bob;
    User carol;
    Group group;

    @BeforeEach
    void setUp() {
        expenseService = new ExpenseService(expenseRepository, userGroupRepository, new GroupAccess(userGroupRepository),
                Mappers.getMapper(ExpenseMapper.class), new SplitCalculator(new Random(42)));
        anna = user("Anna");
        bob = user("Bob");
        carol = user("Carol");
        group = new Group("Trip", "EUR", anna);
        ReflectionTestUtils.setField(group, "id", groupId);
        lenient().when(expenseRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // --- create ---

    @Test
    void splitsEquallyByDefault() {
        givenMembers(anna, bob, carol);

        ExpenseResponse response = expenseService.create(anna.getId(), groupId, equal("10.00", anna, anna, bob, carol));

        assertThat(response.splitType()).isEqualTo(SplitType.EQUAL);
        assertThat(amounts(response)).containsExactlyInAnyOrder("3.34", "3.33", "3.33");
        assertThat(response.splits()).extracting(SplitResponse::value).containsOnlyNulls();
    }

    @Test
    void splitsBySharesAndKeepsTheEnteredValues() {
        givenMembers(anna, bob);

        ExpenseResponse response = expenseService.create(anna.getId(), groupId,
                request("90.00", anna, SplitType.SHARES, Map.of(anna, "2", bob, "1")));

        assertThat(response.splits()).extracting(s -> s.amount().toPlainString(), s -> s.value().toPlainString())
                .containsExactlyInAnyOrder(
                        tuple("60.00", "2"),
                        tuple("30.00", "1"));
    }

    @Test
    void exactSplitDoesNotStoreValues() {
        givenMembers(anna, bob);

        ExpenseResponse response = expenseService.create(anna.getId(), groupId,
                request("10.00", anna, SplitType.EXACT, Map.of(anna, "7.50", bob, "2.50")));

        assertThat(amounts(response)).containsExactlyInAnyOrder("7.50", "2.50");
        assertThat(response.splits()).extracting(SplitResponse::value).containsOnlyNulls();
    }

    @Test
    void payerDoesNotHaveToBeAParticipant() {
        givenMembers(anna, bob, carol);

        ExpenseResponse response = expenseService.create(anna.getId(), groupId, equal("20.00", anna, bob, carol));

        assertThat(response.paidById()).isEqualTo(anna.getId());
        assertThat(response.splits()).extracting(SplitResponse::userId).containsExactly(bob.getId(), carol.getId());
    }

    @Test
    void dateDefaultsToToday() {
        givenMembers(anna);

        assertThat(expenseService.create(anna.getId(), groupId, equal("5.00", anna, anna)).date())
                .isEqualTo(LocalDate.now());
    }

    @Test
    void nonMemberGetsNotFound() {
        when(userGroupRepository.findByGroupIdAndUserId(groupId, anna.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.create(anna.getId(), groupId, equal("5.00", anna, anna)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void payerAndParticipantsMustBeMembers() {
        givenMembers(anna, bob);

        assertThatThrownBy(() -> expenseService.create(anna.getId(), groupId, equal("5.00", carol, anna)))
                .isInstanceOf(InvalidRequestException.class)
                .extracting("field").isEqualTo("paidById");
        assertThatThrownBy(() -> expenseService.create(anna.getId(), groupId, equal("5.00", anna, anna, carol)))
                .isInstanceOf(InvalidRequestException.class)
                .extracting("field").isEqualTo("participants");
        verify(expenseRepository, never()).saveAndFlush(any());
    }

    @Test
    void participantsMustNotRepeat() {
        givenMembers(anna, bob);

        assertThatThrownBy(() -> expenseService.create(anna.getId(), groupId, equal("5.00", anna, bob, bob)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("must not contain the same user twice");
    }

    @Test
    void amountMustFitTheCurrencyDecimals() {
        givenMembers(anna);

        assertThatThrownBy(() -> expenseService.create(anna.getId(), groupId, equal("12.505", anna, anna)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("must have at most 2 decimals in EUR");
    }

    // --- get, list ---

    @Test
    void expenseFromAnotherGroupIsNotFound() {
        givenMembers(anna);
        when(expenseRepository.findByIdAndGroupId(expenseId, groupId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.get(anna.getId(), groupId, expenseId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void listRejectsToBeforeFrom() {
        givenMembers(anna);

        assertThatThrownBy(() -> expenseService.list(anna.getId(), groupId,
                LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 1), null, 0, 20))
                .isInstanceOf(InvalidRequestException.class)
                .extracting("field").isEqualTo("to");
    }

    // --- update, delete ---

    @Test
    void creatorCanReplaceTheExpense() {
        givenMembers(bob, anna);
        givenExpenseCreatedBy(bob);

        ExpenseResponse response = expenseService.update(bob.getId(), groupId, expenseId,
                request("30.00", bob, SplitType.PERCENTAGE, Map.of(anna, "50", bob, "50")));

        assertThat(response.amount()).isEqualByComparingTo("30.00");
        assertThat(response.paidById()).isEqualTo(bob.getId());
        assertThat(response.splitType()).isEqualTo(SplitType.PERCENTAGE);
        assertThat(amounts(response)).containsExactlyInAnyOrder("15.00", "15.00");
    }

    @Test
    void ownerCanDeleteSomeoneElsesExpense() {
        givenMembers(anna, bob);
        Expense expense = givenExpenseCreatedBy(bob);

        expenseService.delete(anna.getId(), groupId, expenseId);

        verify(expenseRepository).delete(expense);
    }

    @Test
    void otherMembersCannotEditOrDelete() {
        givenMembers(carol, anna, bob);
        givenExpenseCreatedBy(bob);

        assertThatThrownBy(() -> expenseService.update(carol.getId(), groupId, expenseId, equal("1.00", carol, carol)))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> expenseService.delete(carol.getId(), groupId, expenseId))
                .isInstanceOf(ForbiddenException.class);
        verify(expenseRepository, never()).delete(any(Expense.class));
    }

    // --- helpers ---

    private static User user(String name) {
        User user = new User(name.toLowerCase() + "@example.com", "hash", name);
        // Ids are normally generated by the database
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    // The first user is the one calling the service. Anna is the group owner.
    private void givenMembers(User... users) {
        List<UserGroup> memberships = Arrays.stream(users)
                .map(user -> new UserGroup(group, user, user == anna ? GroupRole.OWNER : GroupRole.MEMBER))
                .toList();
        when(userGroupRepository.findByGroupIdAndUserId(groupId, users[0].getId()))
                .thenReturn(Optional.of(memberships.getFirst()));
        lenient().when(userGroupRepository.findByGroupIdOrderByJoinedAtAsc(groupId)).thenReturn(memberships);
    }

    private Expense givenExpenseCreatedBy(User creator) {
        Expense expense = new Expense(group, "Dinner", 1000, creator, LocalDate.now(), SplitType.EQUAL, creator);
        expense.replaceSplits(List.of(new Expense.Share(creator, 1000, null)));
        when(expenseRepository.findByIdAndGroupId(expenseId, groupId)).thenReturn(Optional.of(expense));
        return expense;
    }

    private static List<String> amounts(ExpenseResponse response) {
        return response.splits().stream().map(split -> split.amount().toPlainString()).toList();
    }

    private static ExpenseRequest equal(String amount, User payer, User... participants) {
        return new ExpenseRequest("Dinner", new BigDecimal(amount), payer.getId(), null,
                Arrays.stream(participants).map(user -> new ParticipantRequest(user.getId(), null)).toList(), null);
    }

    private static ExpenseRequest request(String amount, User payer, SplitType type, Map<User, String> values) {
        return new ExpenseRequest("Dinner", new BigDecimal(amount), payer.getId(), type,
                values.entrySet().stream()
                        .map(e -> new ParticipantRequest(e.getKey().getId(), new BigDecimal(e.getValue())))
                        .toList(),
                null);
    }
}
