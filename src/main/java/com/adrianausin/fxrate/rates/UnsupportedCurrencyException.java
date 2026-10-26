package com.adrianausin.fxrate.rates;

public final class UnsupportedCurrencyException extends FxRateException {

    public UnsupportedCurrencyException(String currency) {
        super("Currency '%s' is not supported".formatted(currency));
    }
}
