package com.adrianausin.fxrate.rates;

import java.time.LocalDate;

public final class InvalidRateDateException extends FxRateException {

    public InvalidRateDateException(LocalDate date, LocalDate earliest, LocalDate latest) {
        super("Rates are only available between %s and %s, got %s".formatted(earliest, latest, date));
    }
}
