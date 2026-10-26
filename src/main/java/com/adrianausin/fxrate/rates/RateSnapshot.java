package com.adrianausin.fxrate.rates;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Exchange rates for one base currency as published on a given date.
 *
 * @param base      ISO 4217 code every rate is quoted against
 * @param date      publication date of the rates (the provider skips weekends and bank holidays)
 * @param rates     units of the target currency for one unit of {@code base}
 * @param fetchedAt when the rates were fetched from the provider
 * @param stale     {@code true} when the provider was unavailable and a previously fetched snapshot is served
 */
public record RateSnapshot(String base, LocalDate date, Map<String, BigDecimal> rates, Instant fetchedAt, boolean stale) {

    public RateSnapshot {
        rates = Map.copyOf(rates);
    }

    public Optional<BigDecimal> rateFor(String currency) {
        if (base.equals(currency)) {
            return Optional.of(BigDecimal.ONE);
        }
        return Optional.ofNullable(rates.get(currency));
    }

    /**
     * Keeps only the requested currencies. An empty set means "all currencies".
     */
    public RateSnapshot only(Set<String> currencies) {
        if (currencies.isEmpty()) {
            return this;
        }
        Map<String, BigDecimal> filtered = new TreeMap<>();
        for (String currency : currencies) {
            filtered.put(currency, rateFor(currency).orElseThrow(() -> new UnsupportedCurrencyException(currency)));
        }
        return new RateSnapshot(base, date, filtered, fetchedAt, stale);
    }

    public RateSnapshot asStale() {
        return new RateSnapshot(base, date, rates, fetchedAt, true);
    }
}
