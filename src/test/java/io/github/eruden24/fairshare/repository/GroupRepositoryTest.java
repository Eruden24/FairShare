package io.github.eruden24.fairshare.repository;

import io.github.eruden24.fairshare.TestcontainersConfiguration;
import io.github.eruden24.fairshare.entity.Group;
import io.github.eruden24.fairshare.entity.UserGroup;
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
    UserGroupRepository userGroupRepository;

    @Test
    void createsGroupWithOwnerAndFindsItByMember() {
        User owner = userRepository.save(new User("owner@example.com", "hash", "Owner"));
        Group group = groupRepository.save(new Group("Trip to Rome", "EUR", owner));
        userGroupRepository.saveAndFlush(new UserGroup(group, owner, GroupRole.OWNER));

        assertThat(group.getId()).isNotNull();
        assertThat(userGroupRepository.existsByGroupIdAndUserId(group.getId(), owner.getId())).isTrue();
        assertThat(userGroupRepository.findByGroupId(group.getId())).hasSize(1);
        assertThat(groupRepository.findAllByMemberId(owner.getId()))
                .extracting(Group::getName)
                .containsExactly("Trip to Rome");
    }

    @Test
    void rejectsDuplicateMembership() {
        User user = userRepository.save(new User("user@example.com", "hash", "User"));
        Group group = groupRepository.save(new Group("Flat", "EUR", user));
        userGroupRepository.saveAndFlush(new UserGroup(group, user, GroupRole.OWNER));

        assertThatThrownBy(() -> userGroupRepository.saveAndFlush(new UserGroup(group, user, GroupRole.MEMBER)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}