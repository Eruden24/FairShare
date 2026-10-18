package io.github.eruden24.fairshare.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    /** The current time, as a bean so tests can move it forward instead of waiting. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
