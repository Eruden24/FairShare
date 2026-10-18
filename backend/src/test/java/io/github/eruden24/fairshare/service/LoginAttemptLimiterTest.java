package io.github.eruden24.fairshare.service;

import io.github.eruden24.fairshare.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptLimiterTest {

    private static final String EMAIL = "anna@example.com";

    /** A clock the test can move forward. */
    private static final class TestClock extends Clock {
        private Instant now = Instant.parse("2026-10-18T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    private final TestClock clock = new TestClock();
    private final LoginAttemptLimiter limiter = new LoginAttemptLimiter(clock);

    private void fail(int times) {
        for (int i = 0; i < times; i++) {
            limiter.checkAllowed(EMAIL);
            limiter.recordFailure(EMAIL);
        }
    }

    @Test
    void allowsAFewMistakes() {
        fail(LoginAttemptLimiter.MAX_FAILURES - 1);

        assertThatCode(() -> limiter.checkAllowed(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void blocksAfterTooManyFailuresAndSaysWhenToRetry() {
        fail(LoginAttemptLimiter.MAX_FAILURES);
        clock.advance(Duration.ofMinutes(5));

        assertThatThrownBy(() -> limiter.checkAllowed(EMAIL))
                .isInstanceOf(TooManyRequestsException.class)
                .extracting("retryAfter").isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void unblocksWhenTheWindowEnds() {
        fail(LoginAttemptLimiter.MAX_FAILURES);
        clock.advance(LoginAttemptLimiter.WINDOW);

        assertThatCode(() -> limiter.checkAllowed(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void successResetsTheCount() {
        fail(LoginAttemptLimiter.MAX_FAILURES - 1);
        limiter.recordSuccess(EMAIL);
        fail(LoginAttemptLimiter.MAX_FAILURES - 1);

        assertThatCode(() -> limiter.checkAllowed(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void emailsAreCountedSeparately() {
        fail(LoginAttemptLimiter.MAX_FAILURES);

        assertThatCode(() -> limiter.checkAllowed("bob@example.com")).doesNotThrowAnyException();
    }
}
