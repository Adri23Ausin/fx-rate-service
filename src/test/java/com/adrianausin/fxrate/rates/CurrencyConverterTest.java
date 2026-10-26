package com.adrianausin.fxrate.rates;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import com.adrianausin.fxrate.config.FxMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CurrencyConverterTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 23);
    private static final RateSnapshot EUR_RATES = new RateSnapshot("EUR", DATE, Map.of(
            "USD", new BigDecimal("1.1193"),
            "JPY", new BigDecimal("170.12"),
            "KWD", new BigDecimal("0.34391")), Instant.EPOCH, false);

    @Mock
    RateService rateService;

    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    FxMetrics metrics = new FxMetrics(registry);

    @ParameterizedTest
    @CsvSource({
            "USD, 100,    111.93",
            "USD, 0.01,   0.01",
            "JPY, 10.25,  1744",
            "KWD, 1234.5, 424.557",
            "EUR, 99.99,  99.99"
    })
    void roundsToTheMinorUnitOfTheTargetCurrency(String to, BigDecimal amount, BigDecimal expected) {
        given(rateService.latest("EUR")).willReturn(EUR_RATES);

        ConversionResult result = new CurrencyConverter(rateService, metrics).convert("EUR", to, amount, null);

        assertThat(result.result()).isEqualTo(expected);
    }

    @Test
    void usesBankersRounding() {
        RateSnapshot rates = new RateSnapshot("EUR", DATE, Map.of("USD", new BigDecimal("0.5")), Instant.EPOCH, false);
        given(rateService.latest("EUR")).willReturn(rates);
        CurrencyConverter converter = new CurrencyConverter(rateService, metrics);

        // 0.125 and 0.135 sit exactly halfway: HALF_EVEN rounds towards the even digit
        assertThat(converter.convert("EUR", "USD", new BigDecimal("0.25"), null).result()).isEqualTo("0.12");
        assertThat(converter.convert("EUR", "USD", new BigDecimal("0.27"), null).result()).isEqualTo("0.14");
    }

    @Test
    void usesHistoricalRatesWhenADateIsGiven() {
        given(rateService.historical("EUR", DATE)).willReturn(EUR_RATES);

        ConversionResult result = new CurrencyConverter(rateService, metrics).convert("eur", "usd", BigDecimal.TEN, DATE);

        assertThat(result).isEqualTo(new ConversionResult("EUR", "USD", BigDecimal.TEN,
                new BigDecimal("11.19"), new BigDecimal("1.1193"), DATE, false));
    }

    @Test
    void countsConversionsPerCurrencyPair() {
        given(rateService.latest("EUR")).willReturn(EUR_RATES);
        CurrencyConverter converter = new CurrencyConverter(rateService, metrics);

        converter.convert("EUR", "USD", BigDecimal.ONE, null);
        converter.convert("EUR", "USD", BigDecimal.TEN, null);

        assertThat(registry.get("fx.conversions").tag("from", "EUR").tag("to", "USD").counter().count())
                .isEqualTo(2);
    }

    @Test
    void propagatesTheStaleFlag() {
        given(rateService.latest("EUR")).willReturn(EUR_RATES.asStale());

        assertThat(new CurrencyConverter(rateService, metrics).convert("EUR", "USD", BigDecimal.ONE, null).stale()).isTrue();
    }

    @Test
    void failsWhenTheTargetCurrencyHasNoRate() {
        given(rateService.latest("EUR")).willReturn(EUR_RATES);

        assertThatThrownBy(() -> new CurrencyConverter(rateService, metrics).convert("EUR", "CHF", BigDecimal.ONE, null))
                .isInstanceOf(UnsupportedCurrencyException.class);
    }
}
