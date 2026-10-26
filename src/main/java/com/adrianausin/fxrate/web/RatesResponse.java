package com.adrianausin.fxrate.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;

import com.adrianausin.fxrate.rates.RateSnapshot;

record RatesResponse(String base, LocalDate date, Map<String, BigDecimal> rates, Instant fetchedAt, boolean stale) {

    static RatesResponse from(RateSnapshot snapshot) {
        return new RatesResponse(snapshot.base(), snapshot.date(), new TreeMap<>(snapshot.rates()),
                snapshot.fetchedAt(), snapshot.stale());
    }
}
