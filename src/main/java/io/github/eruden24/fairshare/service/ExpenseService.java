package io.github.eruden24.fairshare.service;

import io.github.eruden24.fairshare.dto.expense.CreateExpenseRequest;
import io.github.eruden24.fairshare.dto.expense.ExpenseResponse;
import io.github.eruden24.fairshare.entity.Expense;
import io.github.eruden24.fairshare.entity.Group;
import io.github.eruden24.fairshare.entity.User;
import io.github.eruden24.fairshare.entity.UserGroup;
import io.github.eruden24.fairshare.exception.InvalidRequestException;
import io.github.eruden24.fairshare.exception.NotFoundException;
import io.github.eruden24.fairshare.mapper.ExpenseMapper;
import io.github.eruden24.fairshare.money.Money;
import io.github.eruden24.fairshare.repository.ExpenseRepository;
import io.github.eruden24.fairshare.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

/**
 * Any member of a group can add and view its expenses. The payer and all participants
 * must be members of the group too.
 */
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserGroupRepository userGroupRepository;
    private final GroupAccess groupAccess;
    private final ExpenseMapper expenseMapper;
    private final RandomGenerator splitRandom;

    @Transactional
    public ExpenseResponse create(UUID currentUserId, UUID groupId, CreateExpenseRequest request) {
        UserGroup membership = groupAccess.requireMember(groupId, currentUserId);
        Group group = membership.getGroup();

        Map<UUID, User> members = userGroupRepository.findByGroupIdOrderByJoinedAtAsc(groupId).stream()
                .collect(Collectors.toMap(m -> m.getUser().getId(), UserGroup::getUser));

        User payer = members.get(request.paidById());
        if (payer == null) {
            throw new InvalidRequestException("paidById", "is not a member of this group");
        }
        List<User> participants = findParticipants(request.participantIds(), members);
        Money amount = toMoney(request.amount(), group.getCurrency());

        Expense expense = new Expense(group, request.description().trim(), amount.minorUnits(), payer,
                request.date() != null ? request.date() : LocalDate.now(), membership.getUser());

        // Shares come back in the same order as the participants
        List<Money> shares = amount.splitEqually(participants.size(), splitRandom);
        for (int i = 0; i < participants.size(); i++) {
            expense.addSplit(participants.get(i), shares.get(i).minorUnits());
        }

        return expenseMapper.toResponse(expenseRepository.saveAndFlush(expense));
    }

    @Transactional(readOnly = true)
    public ExpenseResponse get(UUID currentUserId, UUID groupId, UUID expenseId) {
        groupAccess.requireMember(groupId, currentUserId);

        return expenseRepository.findByIdAndGroupId(expenseId, groupId)
                .map(expenseMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Expense not found: " + expenseId));
    }

    private List<User> findParticipants(List<UUID> participantIds, Map<UUID, User> members) {
        if (new HashSet<>(participantIds).size() != participantIds.size()) {
            throw new InvalidRequestException("participantIds", "must not contain duplicates");
        }

        return participantIds.stream()
                .map(id -> {
                    User user = members.get(id);
                    if (user == null) {
                        throw new InvalidRequestException("participantIds", "is not a member of this group: " + id);
                    }
                    return user;
                })
                .toList();
    }

    private static Money toMoney(BigDecimal amount, String currencyCode) {
        Currency currency = Currency.getInstance(currencyCode);
        try {
            return Money.of(amount, currency);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("amount",
                    "must have at most " + currency.getDefaultFractionDigits() + " decimals in " + currencyCode);
        }
    }
}
