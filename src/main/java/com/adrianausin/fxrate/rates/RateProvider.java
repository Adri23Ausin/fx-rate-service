package com.adrianausin.fxrate.rates;

import java.time.LocalDate;

/**
 * Source of exchange rates. Implementations talk to an external provider.
 */
public interface RateProvider {

    RateSnapshot latest(String base);

    RateSnapshot historical(String base, LocalDate date);
}
