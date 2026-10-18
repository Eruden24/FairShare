package io.github.eruden24.fairshare.service;

import io.github.eruden24.fairshare.exception.TooManyRequestsException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Slows down password guessing: after MAX_FAILURES wrong passwords for an email within WINDOW, logins
 * for that email are refused (429) until the window ends, even with the right password. A successful
 * login clears the count.
 * <p>
 * Kept in memory, so it is per app instance and resets on restart. Enough for one instance; with several,
 * this would move to a shared store such as Redis.
 */
@Component
public class LoginAttemptLimiter {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);

    // Failed attempts in the current window, per normalized email
    private record Failures(int count, Instant windowStart) {}

    private final Map<String, Failures> failures = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptLimiter(Clock clock) {
        this.clock = clock;
    }

    /** @throws TooManyRequestsException if this email has too many recent failures */
    public void checkAllowed(String email) {
        Failures current = failures.get(email);
        if (current == null || isExpired(current)) {
            return;
        }
        if (current.count() >= MAX_FAILURES) {
            Duration retryAfter = Duration.between(clock.instant(), current.windowStart().plus(WINDOW));
            throw new TooManyRequestsException("Too many failed login attempts; try again later", retryAfter);
        }
    }

    public void recordFailure(String email) {
        failures.merge(email, new Failures(1, clock.instant()),
                (old, first) -> isExpired(old) ? first : new Failures(old.count() + 1, old.windowStart()));
        // Forget expired entries now and then, so emails nobody uses again don't stay in memory forever
        if (failures.size() > 10_000) {
            failures.values().removeIf(this::isExpired);
        }
    }

    public void recordSuccess(String email) {
        failures.remove(email);
    }

    private boolean isExpired(Failures entry) {
        return !clock.instant().isBefore(entry.windowStart().plus(WINDOW));
    }
}
