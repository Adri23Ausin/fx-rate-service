package com.adrianausin.fxrate.provider.frankfurter;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

import com.adrianausin.fxrate.config.CacheConfig;
import com.adrianausin.fxrate.config.FxMetrics;
import com.adrianausin.fxrate.rates.FxRateException;
import com.adrianausin.fxrate.rates.RateProvider;
import com.adrianausin.fxrate.rates.RateSnapshot;
import com.adrianausin.fxrate.rates.RatesUnavailableException;
import com.adrianausin.fxrate.rates.UnsupportedCurrencyException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Rate provider backed by Frankfurter. Calls go through, from the outside in:
 * cache → circuit breaker (with fallback) → retry → HTTP call.
 */
@Component
class FrankfurterRateProvider implements RateProvider {

    static final String RESILIENCE_INSTANCE = "frankfurter";

    private static final Logger log = LoggerFactory.getLogger(FrankfurterRateProvider.class);

    private final FrankfurterClient client;
    private final LastKnownRates lastKnownRates;
    private final FxMetrics metrics;
    private final Clock clock;

    FrankfurterRateProvider(FrankfurterClient client, LastKnownRates lastKnownRates, FxMetrics metrics, Clock clock) {
        this.client = client;
        this.lastKnownRates = lastKnownRates;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.LATEST_RATES, unless = "#result.stale")
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "latestFallback")
    @Retry(name = RESILIENCE_INSTANCE)
    public RateSnapshot latest(String base) {
        try {
            RateSnapshot snapshot = toSnapshot(client.latest(base));
            lastKnownRates.remember(snapshot);
            return snapshot;
        } catch (HttpClientErrorException.NotFound e) {
            throw new UnsupportedCurrencyException(base);
        }
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.HISTORICAL_RATES, unless = "#result.stale")
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "historicalFallback")
    @Retry(name = RESILIENCE_INSTANCE)
    public RateSnapshot historical(String base, LocalDate date) {
        try {
            return toSnapshot(client.historical(date, base));
        } catch (HttpClientErrorException.NotFound e) {
            throw new UnsupportedCurrencyException(base);
        }
    }

    RateSnapshot latestFallback(String base, Throwable error) {
        rethrowBusinessErrors(error);
        Optional<RateSnapshot> lastKnown = lastKnownRates.latest(base);
        if (lastKnown.isEmpty()) {
            metrics.providerFallback("latest", "unavailable");
            throw new RatesUnavailableException(base, error);
        }
        log.warn("Frankfurter unavailable, serving last known rates for {}: {}", base, error.toString());
        metrics.providerFallback("latest", "stale");
        return lastKnown.get();
    }

    RateSnapshot historicalFallback(String base, LocalDate date, Throwable error) {
        rethrowBusinessErrors(error);
        log.warn("Frankfurter unavailable, no fallback for {} rates on {}: {}", base, date, error.toString());
        metrics.providerFallback("historical", "unavailable");
        throw new RatesUnavailableException(base, error);
    }

    private static void rethrowBusinessErrors(Throwable error) {
        // The fallback is invoked for every exception, but an unknown currency is not an outage
        if (error instanceof FxRateException businessError) {
            throw businessError;
        }
    }

    private RateSnapshot toSnapshot(FrankfurterRatesResponse response) {
        return new RateSnapshot(response.base(), response.date(), response.rates(), clock.instant(), false);
    }
}
