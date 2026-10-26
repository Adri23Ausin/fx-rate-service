package com.adrianausin.fxrate.rates;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

@Service
public class RateService {

    /** First day the ECB reference rates are available. */
    static final LocalDate EARLIEST_DATE = LocalDate.of(1999, 1, 4);

    private final RateProvider provider;
    private final Clock clock;

    public RateService(RateProvider provider, Clock clock) {
        this.provider = provider;
        this.clock = clock;
    }

    public RateSnapshot latest(String base) {
        return provider.latest(CurrencyCodes.normalize(base));
    }

    public RateSnapshot historical(String base, LocalDate date) {
        LocalDate today = LocalDate.now(clock);
        if (date.isBefore(EARLIEST_DATE) || date.isAfter(today)) {
            throw new InvalidRateDateException(date, EARLIEST_DATE, today);
        }
        return provider.historical(CurrencyCodes.normalize(base), date);
    }
}
