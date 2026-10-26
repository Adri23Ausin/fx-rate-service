package com.adrianausin.fxrate.rates;

import java.util.Currency;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Normalises user input into ISO 4217 currency codes.
 */
public final class CurrencyCodes {

    private CurrencyCodes() {
    }

    public static String normalize(String code) {
        if (code == null || code.isBlank()) {
            throw new UnsupportedCurrencyException(String.valueOf(code));
        }
        String upper = code.strip().toUpperCase(Locale.ROOT);
        try {
            return Currency.getInstance(upper).getCurrencyCode();
        } catch (IllegalArgumentException e) {
            throw new UnsupportedCurrencyException(upper);
        }
    }

    public static Set<String> normalize(Set<String> codes) {
        return codes.stream()
                .filter(code -> !code.isBlank())
                .map(CurrencyCodes::normalize)
                .collect(Collectors.toUnmodifiableSet());
    }
}
