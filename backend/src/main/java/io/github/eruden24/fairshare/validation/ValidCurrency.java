package io.github.eruden24.fairshare.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An ISO 4217 currency code in capitals, like EUR. Null is valid: combine with @NotBlank when required.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CurrencyValidator.class)
public @interface ValidCurrency {

    String message() default "must be a valid currency code like EUR";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
