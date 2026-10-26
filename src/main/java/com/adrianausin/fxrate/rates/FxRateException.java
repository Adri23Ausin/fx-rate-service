package com.adrianausin.fxrate.rates;

/**
 * Base type for the business errors of the service. Sealed so the web layer can map
 * every case exhaustively with a pattern-matching {@code switch}.
 */
public abstract sealed class FxRateException extends RuntimeException
        permits UnsupportedCurrencyException, InvalidRateDateException, RatesUnavailableException {

    protected FxRateException(String message) {
        super(message);
    }

    protected FxRateException(String message, Throwable cause) {
        super(message, cause);
    }
}
