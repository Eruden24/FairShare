package io.github.eruden24.fairshare.exception;

import lombok.Getter;

import java.time.Duration;

/** Returned as 429 with a Retry-After header saying when to try again. */
@Getter
public class TooManyRequestsException extends RuntimeException {

    private final Duration retryAfter;

    public TooManyRequestsException(String message, Duration retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }
}
