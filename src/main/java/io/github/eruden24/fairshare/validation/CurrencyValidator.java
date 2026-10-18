package io.github.eruden24.fairshare.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Currency;

public class CurrencyValidator implements ConstraintValidator<ValidCurrency, String> {

    @Override
    public boolean isValid(String code, ConstraintValidatorContext context) {
        if (code == null) {
            return true;
        }
        try {
            // Codes like XAU (gold) or XXX (no currency) exist in ISO 4217 but have no
            // minor units, so they can't hold money amounts.
            return Currency.getInstance(code).getDefaultFractionDigits() >= 0;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
