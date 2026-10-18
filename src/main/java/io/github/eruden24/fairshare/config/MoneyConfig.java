package io.github.eruden24.fairshare.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;
import java.util.random.RandomGenerator;

@Configuration
public class MoneyConfig {

    /**
     * Picks who gets the leftover cent when splitting. java.util.Random is thread-safe, which matters
     * because this single bean is shared by all requests. Tests pass a seeded Random instead.
     */
    @Bean
    RandomGenerator splitRandom() {
        return new Random();
    }
}
