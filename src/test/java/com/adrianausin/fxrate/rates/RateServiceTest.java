package com.adrianausin.fxrate.rates;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RateServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-26T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    RateProvider provider;

    @Test
    void normalisesTheBaseCurrency() {
        new RateService(provider, CLOCK).latest(" usd ");

        verify(provider).latest("USD");
    }

    @Test
    void rejectsCodesThatAreNotIso4217() {
        RateService service = new RateService(provider, CLOCK);

        assertThatThrownBy(() -> service.latest("EURO")).isInstanceOf(UnsupportedCurrencyException.class);
        verifyNoInteractions(provider);
    }

    @Test
    void rejectsDatesInTheFuture() {
        RateService service = new RateService(provider, CLOCK);

        assertThatThrownBy(() -> service.historical("EUR", LocalDate.of(2026, 10, 27)))
                .isInstanceOf(InvalidRateDateException.class);
    }

    @Test
    void rejectsDatesBeforeTheEuro() {
        RateService service = new RateService(provider, CLOCK);

        assertThatThrownBy(() -> service.historical("EUR", LocalDate.of(1999, 1, 1)))
                .isInstanceOf(InvalidRateDateException.class);
    }

    @Test
    void acceptsToday() {
        new RateService(provider, CLOCK).historical("EUR", LocalDate.of(2026, 10, 26));

        verify(provider).historical("EUR", LocalDate.of(2026, 10, 26));
    }
}
