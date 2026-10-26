package com.adrianausin.fxrate.provider.frankfurter;

import java.time.Clock;
import java.time.LocalDate;

import com.adrianausin.fxrate.rates.RateProvider;
import com.adrianausin.fxrate.rates.RateSnapshot;
import org.springframework.stereotype.Component;

@Component
class FrankfurterRateProvider implements RateProvider {

    private final FrankfurterClient client;
    private final Clock clock;

    FrankfurterRateProvider(FrankfurterClient client, Clock clock) {
        this.client = client;
        this.clock = clock;
    }

    @Override
    public RateSnapshot latest(String base) {
        return toSnapshot(client.latest(base));
    }

    @Override
    public RateSnapshot historical(String base, LocalDate date) {
        return toSnapshot(client.historical(date, base));
    }

    private RateSnapshot toSnapshot(FrankfurterRatesResponse response) {
        return new RateSnapshot(response.base(), response.date(), response.rates(), clock.instant(), false);
    }
}
