package io.github.eruden24.fairshare.repository;

import io.github.eruden24.fairshare.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import io.github.eruden24.fairshare.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
public class UserRepositoryTest {

    @Autowired
    UserRepository userRepository;

    @Test
    void savesUserAndFindsByEmailIgnoringCase() {
        User user  = new User("Eruden@example.com", "dummy_hash", "Erud24");

        User saved = userRepository.saveAndFlush(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getDefaultCurrency()).isEqualTo("EUR");
        assertThat(userRepository.findByEmailIgnoreCase("eruden@example.com")).isPresent();
        assertThat(userRepository.existsByEmailIgnoreCase("ERUDEN@EXAMPLE.COM")).isTrue();


    }
}
