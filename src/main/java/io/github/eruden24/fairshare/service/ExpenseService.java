package io.github.eruden24.fairshare.service;

import io.github.eruden24.fairshare.dto.PageResponse;
import io.github.eruden24.fairshare.dto.expense.ExpenseRequest;
import io.github.eruden24.fairshare.dto.expense.ExpenseResponse;
import io.github.eruden24.fairshare.dto.expense.ParticipantRequest;
import io.github.eruden24.fairshare.entity.Expense;
import io.github.eruden24.fairshare.entity.Group;
import io.github.eruden24.fairshare.entity.User;
import io.github.eruden24.fairshare.entity.UserGroup;
import io.github.eruden24.fairshare.enumeration.GroupRole;
import io.github.eruden24.fairshare.enumeration.SplitType;
import io.github.eruden24.fairshare.exception.ConflictException;
import io.github.eruden24.fairshare.exception.ForbiddenException;
import io.github.eruden24.fairshare.exception.InvalidRequestException;
import io.github.eruden24.fairshare.exception.NotFoundException;
import io.github.eruden24.fairshare.mapper.ExpenseMapper;
import io.github.eruden24.fairshare.money.Money;
import io.github.eruden24.fairshare.repository.ExpenseRepository;
import io.github.eruden24.fairshare.repository.ExpenseSpecifications;
import io.github.eruden24.fairshare.repository.UserGroupRepository;
import io.github.eruden24.fairshare.split.SplitCalculator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Any member of a group can add and view its expenses. Only the member who created an expense,
 * or the group owner, can change or delete it. The payer and all participants must be members.
 */
@Service
@RequiredArgsConstructor
public class ExpenseService {

    // Newest first; id last so expenses created at the same moment still come in a stable order
    private static final Sort NEWEST_FIRST = Sort.by(
            Sort.Order.desc("expenseDate"), Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final ExpenseRepository expenseRepository;
    private final UserGroupRepository userGroupRepository;
    private final GroupAccess groupAccess;
    private final ExpenseMapper expenseMapper;
    private final SplitCalculator splitCalculator;
    private final EntityManager entityManager;

    @Transactional
    public ExpenseResponse create(UUID currentUserId, UUID groupId, ExpenseRequest request) {
        UserGroup membership = groupAccess.requireMember(groupId, currentUserId);
        Group group = membership.getGroup();

        PreparedExpense prepared = prepare(request, group);

        Expense expense = new Expense(group, prepared.description(), prepared.amount().minorUnits(), prepared.payer(),
                prepared.date(), prepared.splitType(), membership.getUser());
        expense.replaceSplits(prepared.shares());

        return expenseMapper.toResponse(expenseRepository.saveAndFlush(expense));
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> list(UUID currentUserId, UUID groupId, LocalDate from, LocalDate to,
                                              UUID paidById, int page, int size) {
        groupAccess.requireMember(groupId, currentUserId);
        if (from != null && to != null && to.isBefore(from)) {
            throw new InvalidRequestException("to", "must not be before from");
        }

        return PageResponse.of(expenseRepository
                .findAll(ExpenseSpecifications.matching(groupId, from, to, paidById), PageRequest.of(page, size, NEWEST_FIRST))
                .map(expenseMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public ExpenseResponse get(UUID currentUserId, UUID groupId, UUID expenseId) {
        groupAccess.requireMember(groupId, currentUserId);
        return expenseMapper.toResponse(findExpense(groupId, expenseId));
    }

    /** Replaces the whole expense, so the shares are always recomputed from what's sent. */
    @Transactional
    public ExpenseResponse update(UUID currentUserId, UUID groupId, UUID expenseId, ExpenseRequest request) {
        UserGroup membership = groupAccess.requireMember(groupId, currentUserId);
        Expense expense = findExpense(groupId, expenseId);
        requireCanModify(membership, expense, "edit");

        if (request.version() != null && request.version() != expense.getVersion()) {
            throw new ConflictException("This expense was changed by someone else since you loaded it; "
                    + "reload it (current version " + expense.getVersion() + ") and try again");
        }
        // Lock the row and bump the version right away. Without this, an edit that only changes the shares
        // wouldn't touch the expenses row, so its version wouldn't change. A concurrent edit of the same
        // expense now waits for this one and then fails with 409.
        entityManager.lock(expense, LockModeType.PESSIMISTIC_FORCE_INCREMENT);

        PreparedExpense prepared = prepare(request, membership.getGroup());

        expense.setDescription(prepared.description());
        expense.setAmountMinor(prepared.amount().minorUnits());
        expense.setPaidBy(prepared.payer());
        expense.setExpenseDate(prepared.date());
        expense.setSplitType(prepared.splitType());
        expense.replaceSplits(prepared.shares());

        return expenseMapper.toResponse(expenseRepository.saveAndFlush(expense));
    }

    @Transactional
    public void delete(UUID currentUserId, UUID groupId, UUID expenseId) {
        UserGroup membership = groupAccess.requireMember(groupId, currentUserId);
        Expense expense = findExpense(groupId, expenseId);
        requireCanModify(membership, expense, "delete");

        expenseRepository.delete(expense);
    }

    /** A request that has been checked against the group, with its shares computed. */
    private record PreparedExpense(String description, Money amount, User payer, LocalDate date,
                                   SplitType splitType, List<Expense.Share> shares) {}

    private PreparedExpense prepare(ExpenseRequest request, Group group) {
        Map<UUID, User> members = userGroupRepository.findByGroupIdOrderByJoinedAtAsc(group.getId()).stream()
                .collect(Collectors.toMap(m -> m.getUser().getId(), UserGroup::getUser));

        User payer = members.get(request.paidById());
        if (payer == null) {
            throw new InvalidRequestException("paidById", "is not a member of this group");
        }
        List<User> participants = findParticipants(request.participants(), members);
        Money amount = Amounts.toMoney(request.amount(), group.getCurrency());
        SplitType splitType = request.splitType() != null ? request.splitType() : SplitType.EQUAL;

        List<BigDecimal> values = request.participants().stream().map(ParticipantRequest::value).toList();
        // Shares come back in the same order as the participants
        List<Money> shares = splitCalculator.split(splitType, amount, values);

        // Only PERCENTAGE and SHARES keep what was entered; for EXACT the share already is the value
        boolean keepValues = splitType == SplitType.PERCENTAGE || splitType == SplitType.SHARES;
        List<Expense.Share> newShares = new ArrayList<>();
        for (int i = 0; i < participants.size(); i++) {
            newShares.add(new Expense.Share(participants.get(i), shares.get(i).minorUnits(),
                    keepValues ? values.get(i) : null));
        }

        return new PreparedExpense(request.description().trim(), amount, payer,
                request.date() != null ? request.date() : LocalDate.now(), splitType, newShares);
    }

    private Expense findExpense(UUID groupId, UUID expenseId) {
        return expenseRepository.findByIdAndGroupId(expenseId, groupId).orElseThrow(
                () -> new NotFoundException("Expense not found: " + expenseId)
        );
    }

    private static void requireCanModify(UserGroup membership, Expense expense, String action) {
        boolean isCreator = expense.getCreatedBy().getId().equals(membership.getUser().getId());
        if (!isCreator && membership.getRole() != GroupRole.OWNER) {
            throw new ForbiddenException("Only the member who added this expense or the group owner can " + action + " it");
        }
    }

    private static List<User> findParticipants(List<ParticipantRequest> participants, Map<UUID, User> members) {
        List<UUID> ids = participants.stream().map(ParticipantRequest::userId).toList();
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new InvalidRequestException("participants", "must not contain the same user twice");
        }

        return ids.stream()
                .map(id -> {
                    User user = members.get(id);
                    if (user == null) {
                        throw new InvalidRequestException("participants", "is not a member of this group: " + id);
                    }
                    return user;
                })
                .toList();
    }
}
