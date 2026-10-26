package com.adrianausin.fxrate.rates;

public final class RatesUnavailableException extends FxRateException {

    public RatesUnavailableException(String base, Throwable cause) {
        super("Rates for '%s' are temporarily unavailable".formatted(base), cause);
    }
}
