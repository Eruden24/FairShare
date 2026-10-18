package io.github.eruden24.fairshare.repository;

import io.github.eruden24.fairshare.TestcontainersConfiguration;
import io.github.eruden24.fairshare.entity.ExpenseGroup;
import io.github.eruden24.fairshare.entity.GroupMember;
import io.github.eruden24.fairshare.enumeration.GroupRole;
import io.github.eruden24.fairshare.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class GroupRepositoryTest {

    @Autowired
    UserRepository userRepository;

    @Autowired
    GroupRepository groupRepository;

    @Autowired
    MemberRepository memberRepository;

    @Test
    void createsGroupWithOwnerAndFindsItByMember() {
        User owner = userRepository.save(new User("owner@example.com", "hash", "Owner"));
        ExpenseGroup group = groupRepository.save(new ExpenseGroup("Trip to Rome", "EUR", owner));
        memberRepository.saveAndFlush(new GroupMember(group, owner, GroupRole.OWNER));

        assertThat(group.getId()).isNotNull();
        assertThat(memberRepository.existsByGroupIdAndUserId(group.getId(), owner.getId())).isTrue();
        assertThat(memberRepository.findByGroupId(group.getId())).hasSize(1);
        assertThat(groupRepository.findAllByMemberId(owner.getId()))
                .extracting(ExpenseGroup::getName)
                .containsExactly("Trip to Rome");
    }

    @Test
    void rejectsDuplicateMembership() {
        User user = userRepository.save(new User("user@example.com", "hash", "User"));
        ExpenseGroup group = groupRepository.save(new ExpenseGroup("Flat", "EUR", user));
        memberRepository.saveAndFlush(new GroupMember(group, user, GroupRole.OWNER));

        assertThatThrownBy(() -> memberRepository.saveAndFlush(new GroupMember(group, user, GroupRole.MEMBER)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}