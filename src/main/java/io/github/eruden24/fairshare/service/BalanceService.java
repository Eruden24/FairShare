package io.github.eruden24.fairshare.service;

import io.github.eruden24.fairshare.dto.balance.GroupBalancesResponse;
import io.github.eruden24.fairshare.dto.balance.MemberBalanceResponse;
import io.github.eruden24.fairshare.dto.balance.MyBalanceResponse;
import io.github.eruden24.fairshare.entity.Group;
import io.github.eruden24.fairshare.entity.User;
import io.github.eruden24.fairshare.entity.UserGroup;
import io.github.eruden24.fairshare.money.Money;
import io.github.eruden24.fairshare.repository.ExpenseRepository;
import io.github.eruden24.fairshare.repository.UserGroupRepository;
import io.github.eruden24.fairshare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A balance is what someone paid minus what they owe: their shares of all expenses.
 * Nothing is stored; balances are always computed from the expenses, so they can't drift out of sync.
 */
@Service
@RequiredArgsConstructor
public class BalanceService {

    private final ExpenseRepository expenseRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserRepository userRepository;
    private final GroupAccess groupAccess;

    /** Creditors (positive balance) first, then by name. Members of the group only. */
    @Transactional(readOnly = true)
    public GroupBalancesResponse forGroup(UUID currentUserId, UUID groupId) {
        Group group = groupAccess.requireMember(groupId, currentUserId).getGroup();
        Currency currency = Currency.getInstance(group.getCurrency());

        Map<UUID, Long> paid = expenseRepository.sumPaidPerUser(groupId).stream()
                .collect(Collectors.toMap(ExpenseRepository.UserTotal::getUserId, ExpenseRepository.UserTotal::getTotal));
        Map<UUID, Long> owed = expenseRepository.sumOwedPerUser(groupId).stream()
                .collect(Collectors.toMap(ExpenseRepository.UserTotal::getUserId, ExpenseRepository.UserTotal::getTotal));

        Set<UUID> memberIds = userGroupRepository.findByGroupIdOrderByJoinedAtAsc(groupId).stream()
                .map(m -> m.getUser().getId())
                .collect(Collectors.toSet());

        // Everyone currently in the group, plus anyone who left but still appears in its expenses
        Set<UUID> userIds = new HashSet<>(memberIds);
        userIds.addAll(paid.keySet());
        userIds.addAll(owed.keySet());
        Map<UUID, User> users = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<MemberBalanceResponse> balances = userIds.stream()
                .map(userId -> {
                    Money userPaid = new Money(paid.getOrDefault(userId, 0L), currency);
                    Money userOwed = new Money(owed.getOrDefault(userId, 0L), currency);
                    return new MemberBalanceResponse(userId, users.get(userId).getDisplayName(),
                            memberIds.contains(userId), userPaid.toDecimal(), userOwed.toDecimal(),
                            userPaid.minus(userOwed).toDecimal());
                })
                .sorted(Comparator.comparing(MemberBalanceResponse::balance).reversed()
                        .thenComparing(MemberBalanceResponse::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return new GroupBalancesResponse(groupId, group.getCurrency(), balances);
    }

    /** My balance in each group I'm a member of, in the order I joined them. */
    @Transactional(readOnly = true)
    public List<MyBalanceResponse> forUser(UUID currentUserId) {
        Map<UUID, Long> paid = expenseRepository.sumPaidPerGroup(currentUserId).stream()
                .collect(Collectors.toMap(ExpenseRepository.GroupTotal::getGroupId, ExpenseRepository.GroupTotal::getTotal));
        Map<UUID, Long> owed = expenseRepository.sumOwedPerGroup(currentUserId).stream()
                .collect(Collectors.toMap(ExpenseRepository.GroupTotal::getGroupId, ExpenseRepository.GroupTotal::getTotal));

        return userGroupRepository.findByUserIdOrderByJoinedAtAsc(currentUserId).stream()
                .map(UserGroup::getGroup)
                .map(group -> {
                    Currency currency = Currency.getInstance(group.getCurrency());
                    Money groupPaid = new Money(paid.getOrDefault(group.getId(), 0L), currency);
                    Money groupOwed = new Money(owed.getOrDefault(group.getId(), 0L), currency);
                    return new MyBalanceResponse(group.getId(), group.getName(), group.getCurrency(),
                            groupPaid.toDecimal(), groupOwed.toDecimal(), groupPaid.minus(groupOwed).toDecimal());
                })
                .toList();
    }
}
