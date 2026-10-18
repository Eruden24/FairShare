package io.github.eruden24.fairshare.service;

import io.github.eruden24.fairshare.dto.balance.GroupBalancesResponse;
import io.github.eruden24.fairshare.dto.balance.MemberBalanceResponse;
import io.github.eruden24.fairshare.dto.balance.MyBalanceResponse;
import io.github.eruden24.fairshare.entity.Group;
import io.github.eruden24.fairshare.entity.User;
import io.github.eruden24.fairshare.entity.UserGroup;
import io.github.eruden24.fairshare.money.Money;
import io.github.eruden24.fairshare.repository.ExpenseRepository;
import io.github.eruden24.fairshare.repository.GroupTotal;
import io.github.eruden24.fairshare.repository.SettlementRepository;
import io.github.eruden24.fairshare.repository.UserGroupRepository;
import io.github.eruden24.fairshare.repository.UserRepository;
import io.github.eruden24.fairshare.repository.UserTotal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
 * balance = paid - owed + sent - received
 * <ul>
 *   <li>paid: expenses this person paid</li>
 *   <li>owed: their shares of all expenses</li>
 *   <li>sent / received: settlements they paid to / got from others</li>
 * </ul>
 * Nothing is stored; balances are always computed from expenses and settlements, so they can't drift out of sync.
 */
@Service
@RequiredArgsConstructor
public class BalanceService {

    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserRepository userRepository;
    private final GroupAccess groupAccess;

    /** Creditors (positive balance) first, then by name. Members of the group only. */
    @Transactional(readOnly = true)
    public GroupBalancesResponse forGroup(UUID currentUserId, UUID groupId) {
        Group group = groupAccess.requireMember(groupId, currentUserId).getGroup();
        Currency currency = Currency.getInstance(group.getCurrency());

        Totals totals = totals(groupId);
        Set<UUID> memberIds = memberIds(groupId);

        // Everyone currently in the group, plus anyone who left but still appears in its expenses or settlements
        Set<UUID> userIds = new HashSet<>(memberIds);
        userIds.addAll(totals.userIds());
        Map<UUID, User> users = usersById(userIds);

        List<MemberBalanceResponse> balances = userIds.stream()
                .map(userId -> new MemberBalanceResponse(userId, users.get(userId).getDisplayName(),
                        memberIds.contains(userId),
                        decimal(totals.paid(), userId, currency), decimal(totals.owed(), userId, currency),
                        decimal(totals.sent(), userId, currency), decimal(totals.received(), userId, currency),
                        new Money(totals.balanceOf(userId), currency).toDecimal()))
                .sorted(Comparator.comparing(MemberBalanceResponse::balance).reversed()
                        .thenComparing(MemberBalanceResponse::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return new GroupBalancesResponse(groupId, group.getCurrency(), balances);
    }

    /** My balance in each group I'm a member of, in the order I joined them. */
    @Transactional(readOnly = true)
    public List<MyBalanceResponse> forUser(UUID currentUserId) {
        Map<UUID, Long> paid = byGroup(expenseRepository.sumPaidPerGroup(currentUserId));
        Map<UUID, Long> owed = byGroup(expenseRepository.sumOwedPerGroup(currentUserId));
        Map<UUID, Long> sent = byGroup(settlementRepository.sumSentPerGroup(currentUserId));
        Map<UUID, Long> received = byGroup(settlementRepository.sumReceivedPerGroup(currentUserId));

        return userGroupRepository.findByUserIdOrderByJoinedAtAsc(currentUserId).stream()
                .map(UserGroup::getGroup)
                .map(group -> {
                    Currency currency = Currency.getInstance(group.getCurrency());
                    UUID id = group.getId();
                    long balance = paid.getOrDefault(id, 0L) - owed.getOrDefault(id, 0L)
                            + sent.getOrDefault(id, 0L) - received.getOrDefault(id, 0L);
                    return new MyBalanceResponse(id, group.getName(), group.getCurrency(),
                            decimal(paid, id, currency), decimal(owed, id, currency),
                            decimal(sent, id, currency), decimal(received, id, currency),
                            new Money(balance, currency).toDecimal());
                })
                .toList();
    }

    /**
     * Net balance in minor units of everyone with expenses or settlements in the group; anyone missing is at zero.
     * No access check: for other services that already checked it.
     */
    @Transactional(readOnly = true)
    public Map<UUID, Long> netBalances(UUID groupId) {
        Totals totals = totals(groupId);
        return totals.userIds().stream().collect(Collectors.toMap(Function.identity(), totals::balanceOf));
    }

    /** Display names of the given users, for services that show balances or transfers. */
    @Transactional(readOnly = true)
    public Map<UUID, User> usersById(Set<UUID> userIds) {
        return userRepository.findAllById(userIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private record Totals(Map<UUID, Long> paid, Map<UUID, Long> owed, Map<UUID, Long> sent, Map<UUID, Long> received) {

        long balanceOf(UUID userId) {
            return paid.getOrDefault(userId, 0L) - owed.getOrDefault(userId, 0L)
                    + sent.getOrDefault(userId, 0L) - received.getOrDefault(userId, 0L);
        }

        Set<UUID> userIds() {
            Set<UUID> ids = new HashSet<>(paid.keySet());
            ids.addAll(owed.keySet());
            ids.addAll(sent.keySet());
            ids.addAll(received.keySet());
            return ids;
        }
    }

    private Totals totals(UUID groupId) {
        return new Totals(
                byUser(expenseRepository.sumPaidPerUser(groupId)),
                byUser(expenseRepository.sumOwedPerUser(groupId)),
                byUser(settlementRepository.sumSentPerUser(groupId)),
                byUser(settlementRepository.sumReceivedPerUser(groupId)));
    }

    private Set<UUID> memberIds(UUID groupId) {
        return userGroupRepository.findByGroupIdOrderByJoinedAtAsc(groupId).stream()
                .map(m -> m.getUser().getId())
                .collect(Collectors.toSet());
    }

    private static Map<UUID, Long> byUser(List<UserTotal> totals) {
        return totals.stream().collect(Collectors.toMap(UserTotal::getUserId, UserTotal::getTotal));
    }

    private static Map<UUID, Long> byGroup(List<GroupTotal> totals) {
        return totals.stream().collect(Collectors.toMap(GroupTotal::getGroupId, GroupTotal::getTotal));
    }

    private static BigDecimal decimal(Map<UUID, Long> totals, UUID id, Currency currency) {
        return new Money(totals.getOrDefault(id, 0L), currency).toDecimal();
    }
}
