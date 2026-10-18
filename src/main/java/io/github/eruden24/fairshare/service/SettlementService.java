package io.github.eruden24.fairshare.service;

import io.github.eruden24.fairshare.dto.settlement.SettlementRequest;
import io.github.eruden24.fairshare.dto.settlement.SettlementResponse;
import io.github.eruden24.fairshare.dto.settlement.SuggestedTransferResponse;
import io.github.eruden24.fairshare.entity.Group;
import io.github.eruden24.fairshare.entity.Settlement;
import io.github.eruden24.fairshare.entity.User;
import io.github.eruden24.fairshare.entity.UserGroup;
import io.github.eruden24.fairshare.enumeration.GroupRole;
import io.github.eruden24.fairshare.exception.ForbiddenException;
import io.github.eruden24.fairshare.exception.InvalidRequestException;
import io.github.eruden24.fairshare.exception.NotFoundException;
import io.github.eruden24.fairshare.mapper.SettlementMapper;
import io.github.eruden24.fairshare.money.Money;
import io.github.eruden24.fairshare.repository.SettlementRepository;
import io.github.eruden24.fairshare.repository.UserGroupRepository;
import io.github.eruden24.fairshare.settlement.DebtSimplifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Any member can record a settlement between two members (e.g. "Carol paid Bob 30.00"), not only the
 * people involved: often one person keeps the books for the group. Only the member who recorded it,
 * or the group owner, can delete it.
 */
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final UserGroupRepository userGroupRepository;
    private final GroupAccess groupAccess;
    private final BalanceService balanceService;
    private final SettlementMapper settlementMapper;

    @Transactional
    public SettlementResponse create(UUID currentUserId, UUID groupId, SettlementRequest request) {
        UserGroup membership = groupAccess.requireMember(groupId, currentUserId);
        Group group = membership.getGroup();

        if (request.fromUserId().equals(request.toUserId())) {
            throw new InvalidRequestException("toUserId", "must be different from fromUserId");
        }
        Map<UUID, User> members = userGroupRepository.findByGroupIdOrderByJoinedAtAsc(groupId).stream()
                .collect(Collectors.toMap(m -> m.getUser().getId(), UserGroup::getUser));
        User from = requireMember(members, request.fromUserId(), "fromUserId");
        User to = requireMember(members, request.toUserId(), "toUserId");
        Money amount = Amounts.toMoney(request.amount(), group.getCurrency());

        Settlement settlement = new Settlement(group, from, to, amount.minorUnits(),
                request.date() != null ? request.date() : LocalDate.now(), membership.getUser());

        return settlementMapper.toResponse(settlementRepository.saveAndFlush(settlement));
    }

    /** Newest first. Settlements are few compared to expenses, so the list isn't paginated. */
    @Transactional(readOnly = true)
    public List<SettlementResponse> list(UUID currentUserId, UUID groupId) {
        groupAccess.requireMember(groupId, currentUserId);
        return settlementMapper.toResponse(settlementRepository.findByGroupIdOrderBySettledOnDescCreatedAtDesc(groupId));
    }

    @Transactional
    public void delete(UUID currentUserId, UUID groupId, UUID settlementId) {
        UserGroup membership = groupAccess.requireMember(groupId, currentUserId);
        Settlement settlement = settlementRepository.findByIdAndGroupId(settlementId, groupId).orElseThrow(
                () -> new NotFoundException("Settlement not found: " + settlementId)
        );

        boolean isCreator = settlement.getCreatedBy().getId().equals(currentUserId);
        if (!isCreator && membership.getRole() != GroupRole.OWNER) {
            throw new ForbiddenException("Only the member who recorded this settlement or the group owner can delete it");
        }

        settlementRepository.delete(settlement);
    }

    /** The transfers that would bring every balance in the group to zero. Empty when everyone is settled up. */
    @Transactional(readOnly = true)
    public List<SuggestedTransferResponse> suggest(UUID currentUserId, UUID groupId) {
        Group group = groupAccess.requireMember(groupId, currentUserId).getGroup();
        Currency currency = Currency.getInstance(group.getCurrency());

        List<DebtSimplifier.Transfer> transfers = DebtSimplifier.simplify(balanceService.netBalances(groupId));

        Map<UUID, User> users = balanceService.usersById(transfers.stream()
                .flatMap(t -> Stream.of(t.fromUserId(), t.toUserId()))
                .collect(Collectors.toSet()));

        return transfers.stream()
                .map(t -> new SuggestedTransferResponse(
                        t.fromUserId(), users.get(t.fromUserId()).getDisplayName(),
                        t.toUserId(), users.get(t.toUserId()).getDisplayName(),
                        new Money(t.amountMinor(), currency).toDecimal()))
                .toList();
    }

    private static User requireMember(Map<UUID, User> members, UUID userId, String field) {
        User user = members.get(userId);
        if (user == null) {
            throw new InvalidRequestException(field, "is not a member of this group");
        }
        return user;
    }
}
