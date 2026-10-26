package com.adrianausin.fxrate.provider.frankfurter;

import java.time.Clock;
import java.time.LocalDate;

import com.adrianausin.fxrate.config.CacheConfig;
import com.adrianausin.fxrate.rates.RateProvider;
import com.adrianausin.fxrate.rates.RateSnapshot;
import com.adrianausin.fxrate.rates.UnsupportedCurrencyException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Component
class FrankfurterRateProvider implements RateProvider {

    private final FrankfurterClient client;
    private final Clock clock;

    FrankfurterRateProvider(FrankfurterClient client, Clock clock) {
        this.client = client;
        this.clock = clock;
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.LATEST_RATES, unless = "#result.stale")
    public RateSnapshot latest(String base) {
        try {
            return toSnapshot(client.latest(base));
        } catch (HttpClientErrorException.NotFound e) {
            throw new UnsupportedCurrencyException(base);
        }
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.HISTORICAL_RATES, unless = "#result.stale")
    public RateSnapshot historical(String base, LocalDate date) {
        try {
            return toSnapshot(client.historical(date, base));
        } catch (HttpClientErrorException.NotFound e) {
            throw new UnsupportedCurrencyException(base);
        }
    }

    private RateSnapshot toSnapshot(FrankfurterRatesResponse response) {
        return new RateSnapshot(response.base(), response.date(), response.rates(), clock.instant(), false);
    }
}
