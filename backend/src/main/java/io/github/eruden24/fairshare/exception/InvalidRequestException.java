package io.github.eruden24.fairshare.exception;

import lombok.Getter;

/**
 * A request that passed bean validation but is invalid in context, e.g. a payer who isn't
 * in the group. Returned as 400 with the same "errors" shape as validation failures.
 */
@Getter
public class InvalidRequestException extends RuntimeException {

    private final String field;

    public InvalidRequestException(String field, String message) {
        super(message);
        this.field = field;
    }
}
