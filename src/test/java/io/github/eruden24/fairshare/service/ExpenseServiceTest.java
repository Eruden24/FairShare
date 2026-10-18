package io.github.eruden24.fairshare.service;

import io.github.eruden24.fairshare.dto.expense.CreateExpenseRequest;
import io.github.eruden24.fairshare.dto.expense.ExpenseResponse;
import io.github.eruden24.fairshare.dto.expense.SplitResponse;
import io.github.eruden24.fairshare.entity.Expense;
import io.github.eruden24.fairshare.entity.Group;
import io.github.eruden24.fairshare.entity.User;
import io.github.eruden24.fairshare.entity.UserGroup;
import io.github.eruden24.fairshare.enumeration.GroupRole;
import io.github.eruden24.fairshare.exception.InvalidRequestException;
import io.github.eruden24.fairshare.exception.NotFoundException;
import io.github.eruden24.fairshare.mapper.ExpenseMapper;
import io.github.eruden24.fairshare.repository.ExpenseRepository;
import io.github.eruden24.fairshare.repository.UserGroupRepository;
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
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    User anna;
    User bob;
    User carol;
    Group group;

    @BeforeEach
    void setUp() {
        expenseService = new ExpenseService(expenseRepository, userGroupRepository,
                new GroupAccess(userGroupRepository), Mappers.getMapper(ExpenseMapper.class), new Random(42));
        anna = user("Anna");
        bob = user("Bob");
        carol = user("Carol");
        group = new Group("Trip", "EUR", anna);
    }

    @Test
    void splitsTheAmountEquallyBetweenParticipants() {
        givenMembers(anna, bob, carol);
        when(expenseRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ExpenseResponse response = expenseService.create(anna.getId(), groupId,
                request("10.00", anna, anna, bob, carol));

        assertThat(response.amount()).isEqualByComparingTo("10.00");
        assertThat(response.currency()).isEqualTo("EUR");
        assertThat(response.splits()).extracting(SplitResponse::amount)
                .map(BigDecimal::toPlainString)
                .containsExactlyInAnyOrder("3.34", "3.33", "3.33");
    }

    @Test
    void payerDoesNotHaveToBeAParticipant() {
        givenMembers(anna, bob, carol);
        when(expenseRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ExpenseResponse response = expenseService.create(anna.getId(), groupId,
                request("20.00", anna, bob, carol));

        assertThat(response.paidById()).isEqualTo(anna.getId());
        assertThat(response.splits()).extracting(SplitResponse::userId)
                .containsExactly(bob.getId(), carol.getId());
    }

    @Test
    void dateDefaultsToToday() {
        givenMembers(anna);
        when(expenseRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ExpenseResponse response = expenseService.create(anna.getId(), groupId, request("5.00", anna, anna));

        assertThat(response.date()).isEqualTo(LocalDate.now());
    }

    @Test
    void nonMemberGetsNotFound() {
        when(userGroupRepository.findByGroupIdAndUserId(groupId, anna.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.create(anna.getId(), groupId, request("5.00", anna, anna)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void payerMustBeAMember() {
        givenMembers(anna, bob);

        assertThatThrownBy(() -> expenseService.create(anna.getId(), groupId, request("5.00", carol, anna)))
                .isInstanceOf(InvalidRequestException.class)
                .extracting("field").isEqualTo("paidById");
        verify(expenseRepository, never()).saveAndFlush(any());
    }

    @Test
    void participantsMustBeMembers() {
        givenMembers(anna, bob);

        assertThatThrownBy(() -> expenseService.create(anna.getId(), groupId, request("5.00", anna, anna, carol)))
                .isInstanceOf(InvalidRequestException.class)
                .extracting("field").isEqualTo("participantIds");
    }

    @Test
    void participantsMustNotRepeat() {
        givenMembers(anna, bob);

        assertThatThrownBy(() -> expenseService.create(anna.getId(), groupId, request("5.00", anna, bob, bob)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("must not contain duplicates");
    }

    @Test
    void amountMustFitTheCurrencyDecimals() {
        givenMembers(anna);

        assertThatThrownBy(() -> expenseService.create(anna.getId(), groupId, request("12.505", anna, anna)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("must have at most 2 decimals in EUR");
    }

    @Test
    void expenseFromAnotherGroupIsNotFound() {
        givenMembers(anna);
        UUID expenseId = UUID.randomUUID();
        when(expenseRepository.findByIdAndGroupId(expenseId, groupId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.get(anna.getId(), groupId, expenseId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getsAnExpense() {
        givenMembers(anna);
        Expense expense = new Expense(group, "Taxi", 1250, anna, LocalDate.of(2026, 10, 1), anna);
        expense.addSplit(anna, 1250);
        UUID expenseId = UUID.randomUUID();
        when(expenseRepository.findByIdAndGroupId(expenseId, groupId)).thenReturn(Optional.of(expense));

        ExpenseResponse response = expenseService.get(anna.getId(), groupId, expenseId);

        assertThat(response.description()).isEqualTo("Taxi");
        assertThat(response.amount().toPlainString()).isEqualTo("12.50");
    }

    private static User user(String name) {
        User user = new User(name.toLowerCase() + "@example.com", "hash", name);
        // Ids are normally generated by the database
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    // The first user is the one calling the service
    private void givenMembers(User... users) {
        List<UserGroup> memberships = Arrays.stream(users)
                .map(user -> new UserGroup(group, user, user == anna ? GroupRole.OWNER : GroupRole.MEMBER))
                .toList();
        when(userGroupRepository.findByGroupIdAndUserId(groupId, users[0].getId()))
                .thenReturn(Optional.of(memberships.getFirst()));
        lenient().when(userGroupRepository.findByGroupIdOrderByJoinedAtAsc(groupId))
                .thenReturn(memberships);
    }

    private static CreateExpenseRequest request(String amount, User payer, User... participants) {
        return new CreateExpenseRequest("Dinner", new BigDecimal(amount), payer.getId(),
                Arrays.stream(participants).map(User::getId).toList(), null);
    }
}
