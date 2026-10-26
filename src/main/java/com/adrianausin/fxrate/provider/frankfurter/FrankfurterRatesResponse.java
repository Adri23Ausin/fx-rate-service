package com.adrianausin.fxrate.provider.frankfurter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Payload returned by the Frankfurter API, e.g.
 * {@code {"amount":1.0,"base":"EUR","date":"2026-10-23","rates":{"GBP":0.83728,"USD":1.1193}}}.
 */
record FrankfurterRatesResponse(BigDecimal amount, String base, LocalDate date, Map<String, BigDecimal> rates) {
}
