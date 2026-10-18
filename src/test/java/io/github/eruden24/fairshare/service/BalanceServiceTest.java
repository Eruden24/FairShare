package io.github.eruden24.fairshare.service;

import io.github.eruden24.fairshare.dto.balance.GroupBalancesResponse;
import io.github.eruden24.fairshare.dto.balance.MemberBalanceResponse;
import io.github.eruden24.fairshare.dto.balance.MyBalanceResponse;
import io.github.eruden24.fairshare.entity.Group;
import io.github.eruden24.fairshare.entity.User;
import io.github.eruden24.fairshare.entity.UserGroup;
import io.github.eruden24.fairshare.enumeration.GroupRole;
import io.github.eruden24.fairshare.exception.NotFoundException;
import io.github.eruden24.fairshare.repository.ExpenseRepository;
import io.github.eruden24.fairshare.repository.UserGroupRepository;
import io.github.eruden24.fairshare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock
    ExpenseRepository expenseRepository;

    @Mock
    UserGroupRepository userGroupRepository;

    @Mock
    UserRepository userRepository;

    BalanceService balanceService;

    User anna;
    User bob;
    User carol;
    Group group;

    @BeforeEach
    void setUp() {
        balanceService = new BalanceService(expenseRepository, userGroupRepository, userRepository,
                new GroupAccess(userGroupRepository));
        anna = user("Anna");
        bob = user("Bob");
        carol = user("Carol");
        group = new Group("Trip", "EUR", anna);
        ReflectionTestUtils.setField(group, "id", UUID.randomUUID());
    }

    @Test
    void balanceIsPaidMinusOwedAndAllBalancesAddUpToZero() {
        // Anna paid a 30.00 dinner, Bob a 60.00 taxi, both split between all three
        givenMembers(anna, bob, carol);
        givenTotals(Map.of(anna, 3000L, bob, 6000L), Map.of(anna, 3000L, bob, 3000L, carol, 3000L));

        GroupBalancesResponse response = balanceService.forGroup(anna.getId(), group.getId());

        assertThat(response.currency()).isEqualTo("EUR");
        assertThat(response.balances())
                .extracting(MemberBalanceResponse::displayName, b -> b.paid().toPlainString(),
                        b -> b.owed().toPlainString(), b -> b.balance().toPlainString())
                .containsExactly(
                        tuple("Bob", "60.00", "30.00", "30.00"),
                        tuple("Anna", "30.00", "30.00", "0.00"),
                        tuple("Carol", "0.00", "30.00", "-30.00"));
        assertThat(response.balances().stream().map(MemberBalanceResponse::balance).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("0");
    }

    @Test
    void membersWithoutExpensesHaveZeroBalance() {
        givenMembers(anna, bob);
        givenTotals(Map.of(), Map.of());

        assertThat(balanceService.forGroup(anna.getId(), group.getId()).balances())
                .extracting(MemberBalanceResponse::balance)
                .allSatisfy(balance -> assertThat(balance).isEqualByComparingTo("0"));
    }

    @Test
    void someoneWhoLeftStillShowsUpWithTheirBalance() {
        // Carol left the group but still owes for a dinner
        givenMembers(anna, bob);
        givenTotals(Map.of(anna, 3000L), Map.of(anna, 1000L, bob, 1000L, carol, 1000L));

        assertThat(balanceService.forGroup(anna.getId(), group.getId()).balances())
                .extracting(MemberBalanceResponse::displayName, MemberBalanceResponse::member)
                .containsExactly(tuple("Anna", true), tuple("Bob", true), tuple("Carol", false));
    }

    @Test
    void nonMemberGetsNotFound() {
        when(userGroupRepository.findByGroupIdAndUserId(group.getId(), carol.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> balanceService.forGroup(carol.getId(), group.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void myBalancesListEveryGroupIAmIn() {
        Group empty = new Group("Empty", "JPY", anna);
        ReflectionTestUtils.setField(empty, "id", UUID.randomUUID());
        when(userGroupRepository.findByUserIdOrderByJoinedAtAsc(anna.getId())).thenReturn(List.of(
                new UserGroup(group, anna, GroupRole.OWNER), new UserGroup(empty, anna, GroupRole.OWNER)));
        when(expenseRepository.sumPaidPerGroup(anna.getId())).thenReturn(List.of(groupTotal(group, 3000)));
        when(expenseRepository.sumOwedPerGroup(anna.getId())).thenReturn(List.of(groupTotal(group, 1000)));

        List<MyBalanceResponse> balances = balanceService.forUser(anna.getId());

        assertThat(balances)
                .extracting(MyBalanceResponse::groupName, MyBalanceResponse::currency, b -> b.balance().toPlainString())
                .containsExactly(tuple("Trip", "EUR", "20.00"), tuple("Empty", "JPY", "0"));
    }

    // --- helpers ---

    private static User user(String name) {
        User user = new User(name.toLowerCase() + "@example.com", "hash", name);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    // The first user is the one calling the service
    private void givenMembers(User... users) {
        List<UserGroup> memberships = Arrays.stream(users)
                .map(user -> new UserGroup(group, user, GroupRole.MEMBER))
                .toList();
        when(userGroupRepository.findByGroupIdAndUserId(group.getId(), users[0].getId()))
                .thenReturn(Optional.of(memberships.getFirst()));
        when(userGroupRepository.findByGroupIdOrderByJoinedAtAsc(group.getId())).thenReturn(memberships);
    }

    private void givenTotals(Map<User, Long> paid, Map<User, Long> owed) {
        when(expenseRepository.sumPaidPerUser(group.getId())).thenReturn(userTotals(paid));
        when(expenseRepository.sumOwedPerUser(group.getId())).thenReturn(userTotals(owed));
        when(userRepository.findAllById(any())).thenReturn(List.of(anna, bob, carol));
    }

    private static List<ExpenseRepository.UserTotal> userTotals(Map<User, Long> totals) {
        return totals.entrySet().stream()
                .map(e -> (ExpenseRepository.UserTotal) new ExpenseRepository.UserTotal() {
                    public UUID getUserId() { return e.getKey().getId(); }
                    public long getTotal() { return e.getValue(); }
                })
                .toList();
    }

    private static ExpenseRepository.GroupTotal groupTotal(Group group, long total) {
        return new ExpenseRepository.GroupTotal() {
            public UUID getGroupId() { return group.getId(); }
            public long getTotal() { return total; }
        };
    }
}
