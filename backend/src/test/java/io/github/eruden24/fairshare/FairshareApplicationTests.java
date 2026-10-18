package io.github.eruden24.fairshare;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "fairshare.jwt.secret=test-secret-that-is-at-least-32-characters")
class FairshareApplicationTests {

	@Test
	void contextLoads() {
	}

}
