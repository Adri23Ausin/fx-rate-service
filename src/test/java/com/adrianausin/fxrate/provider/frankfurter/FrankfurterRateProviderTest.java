package com.adrianausin.fxrate.provider.frankfurter;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.adrianausin.fxrate.rates.RateSnapshot;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FrankfurterRateProviderTest {

    @RegisterExtension
    static WireMockExtension frankfurter = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void frankfurterUrl(DynamicPropertyRegistry registry) {
        registry.add("fx.provider.frankfurter.base-url", frankfurter::baseUrl);
    }

    @Autowired
    FrankfurterRateProvider provider;

    @Test
    void mapsLatestRates() {
        frankfurter.stubFor(get(urlEqualTo("/v1/latest?base=EUR")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        {"amount":1.0,"base":"EUR","date":"2026-10-23","rates":{"GBP":0.83728,"USD":1.1193}}
                        """)));

        RateSnapshot snapshot = provider.latest("EUR");

        assertThat(snapshot.base()).isEqualTo("EUR");
        assertThat(snapshot.date()).isEqualTo(LocalDate.of(2026, 10, 23));
        assertThat(snapshot.rates()).containsEntry("USD", new BigDecimal("1.1193"));
        assertThat(snapshot.stale()).isFalse();
    }

    @Test
    void requestsHistoricalRatesByDate() {
        frankfurter.stubFor(get(urlEqualTo("/v1/2024-01-02?base=USD")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        {"amount":1.0,"base":"USD","date":"2024-01-02","rates":{"EUR":0.91274}}
                        """)));

        RateSnapshot snapshot = provider.historical("USD", LocalDate.of(2024, 1, 2));

        assertThat(snapshot.rateFor("EUR")).contains(new BigDecimal("0.91274"));
        assertThat(snapshot.rateFor("USD")).contains(BigDecimal.ONE);
    }
}
