package io.github.eruden24.fairshare;

import org.springframework.boot.SpringApplication;

public class TestFairshareApplication {

	public static void main(String[] args) {
		SpringApplication.from(FairshareApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
