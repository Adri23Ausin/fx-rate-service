package com.adrianausin.fxrate.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import com.adrianausin.fxrate.rates.InvalidRateDateException;
import com.adrianausin.fxrate.rates.RateService;
import com.adrianausin.fxrate.rates.RateSnapshot;
import com.adrianausin.fxrate.rates.UnsupportedCurrencyException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@WebMvcTest(controllers = RateController.class, properties = "fx.rate-limit.enabled=false")
class RateControllerTest {

    private static final RateSnapshot EUR_RATES = new RateSnapshot("EUR", LocalDate.of(2026, 10, 23),
            Map.of("USD", new BigDecimal("1.1193"), "GBP", new BigDecimal("0.83728"), "JPY", new BigDecimal("170.12")),
            Instant.parse("2026-10-23T15:00:00Z"), false);

    @Autowired
    MockMvcTester mvc;

    @MockitoBean
    RateService rateService;

    @Test
    void returnsLatestRatesFilteredBySymbols() {
        given(rateService.latest("eur")).willReturn(EUR_RATES);

        assertThat(mvc.get().uri("/api/v1/rates/latest?base=eur&symbols=usd,gbp"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.rates", rates -> rates.assertThat().asMap().containsOnlyKeys("USD", "GBP"))
                .hasPathSatisfying("$.base", base -> base.assertThat().isEqualTo("EUR"))
                .hasPathSatisfying("$.stale", stale -> stale.assertThat().isEqualTo(false));
    }

    @Test
    void returnsHistoricalRates() {
        given(rateService.historical("EUR", LocalDate.of(2026, 10, 23))).willReturn(EUR_RATES);

        assertThat(mvc.get().uri("/api/v1/rates/2026-10-23"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.rates.JPY").isEqualTo(170.12);
    }

    @Test
    void unknownSymbolIsABadRequest() {
        given(rateService.latest("EUR")).willReturn(EUR_RATES);

        assertThat(mvc.get().uri("/api/v1/rates/latest?symbols=CHF"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("Currency 'CHF' is not supported");
    }

    @Test
    void unsupportedBaseIsABadRequest() {
        given(rateService.latest("XXX")).willThrow(new UnsupportedCurrencyException("XXX"));

        assertThat(mvc.get().uri("/api/v1/rates/latest?base=XXX"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void dateOutOfRangeIsABadRequest() {
        LocalDate date = LocalDate.of(1990, 1, 1);
        given(rateService.historical("EUR", date))
                .willThrow(new InvalidRateDateException(date, LocalDate.of(1999, 1, 4), LocalDate.of(2026, 10, 26)));

        assertThat(mvc.get().uri("/api/v1/rates/1990-01-01"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.title").isEqualTo("Bad Request");
    }

    @Test
    void malformedDateIsABadRequest() {
        assertThat(mvc.get().uri("/api/v1/rates/not-a-date"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }
}
