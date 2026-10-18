package io.github.eruden24.fairshare.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CurrencyValidatorTest {

    record Holder(@ValidCurrency String currency) {}

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"EUR", "USD", "GBP", "JPY", "KWD"})
    void acceptsRealCurrencies(String code) {
        assertThat(validator.validate(new Holder(code))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"XYZ", "eur", "EURO", "EU", "", " EUR", "XAU", "XXX"})
    void rejectsInvalidCodes(String code) {
        Set<ConstraintViolation<Holder>> violations = validator.validate(new Holder(code));

        assertThat(violations).singleElement()
                .extracting(ConstraintViolation::getMessage)
                .isEqualTo("must be a valid currency code like EUR");
    }
}
