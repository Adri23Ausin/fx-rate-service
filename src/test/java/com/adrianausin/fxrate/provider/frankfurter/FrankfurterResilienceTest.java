package com.adrianausin.fxrate.provider.frankfurter;

import java.time.LocalDate;

import com.adrianausin.fxrate.rates.RateSnapshot;
import com.adrianausin.fxrate.rates.RatesUnavailableException;
import com.adrianausin.fxrate.rates.UnsupportedCurrencyException;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "fx.provider.frankfurter.read-timeout=300ms",
        "resilience4j.retry.instances.frankfurter.wait-duration=10ms",
        "resilience4j.circuitbreaker.instances.frankfurter.minimum-number-of-calls=3",
        "resilience4j.circuitbreaker.instances.frankfurter.sliding-window-size=3"
})
class FrankfurterResilienceTest {

    private static final String EUR_LATEST = """
            {"amount":1.0,"base":"EUR","date":"2026-10-23","rates":{"USD":1.1193}}
            """;

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

    @Autowired
    LastKnownRates lastKnownRates;

    @Autowired
    CacheManager cacheManager;

    @Autowired
    CircuitBreakerRegistry circuitBreakers;

    @BeforeEach
    void reset() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        lastKnownRates.clear();
        circuitBreaker().reset();
    }

    @Test
    void retriesTransientServerErrors() {
        frankfurter.stubFor(get(urlEqualTo("/v1/latest?base=EUR")).inScenario("flaky")
                .whenScenarioStateIs(STARTED).willReturn(serverError()).willSetStateTo("recovering"));
        frankfurter.stubFor(get(urlEqualTo("/v1/latest?base=EUR")).inScenario("flaky")
                .whenScenarioStateIs("recovering").willReturn(serverError()).willSetStateTo("up"));
        frankfurter.stubFor(get(urlEqualTo("/v1/latest?base=EUR")).inScenario("flaky")
                .whenScenarioStateIs("up").willReturn(json(EUR_LATEST)));

        RateSnapshot snapshot = provider.latest("EUR");

        assertThat(snapshot.stale()).isFalse();
        frankfurter.verify(3, getRequestedFor(urlPathEqualTo("/v1/latest")));
    }

    @Test
    void servesTheLastKnownRatesWhenTheProviderIsDown() {
        frankfurter.stubFor(get(urlEqualTo("/v1/latest?base=EUR")).willReturn(json(EUR_LATEST)));
        provider.latest("EUR");
        cacheManager.getCache("latestRates").clear();
        frankfurter.stubFor(get(urlEqualTo("/v1/latest?base=EUR")).willReturn(serverError()));

        RateSnapshot snapshot = provider.latest("EUR");

        assertThat(snapshot.stale()).isTrue();
        assertThat(snapshot.rateFor("USD")).isPresent();
    }

    @Test
    void staleRatesAreNotCached() {
        frankfurter.stubFor(get(urlEqualTo("/v1/latest?base=EUR")).willReturn(json(EUR_LATEST)));
        provider.latest("EUR");
        cacheManager.getCache("latestRates").clear();
        frankfurter.stubFor(get(urlEqualTo("/v1/latest?base=EUR")).willReturn(serverError()));
        provider.latest("EUR");

        frankfurter.stubFor(get(urlEqualTo("/v1/latest?base=EUR")).willReturn(json(EUR_LATEST)));

        assertThat(provider.latest("EUR").stale()).isFalse();
    }

    @Test
    void failsWhenThereIsNothingToFallBackTo() {
        frankfurter.stubFor(get(urlEqualTo("/v1/latest?base=GBP")).willReturn(serverError()));

        assertThatThrownBy(() -> provider.latest("GBP")).isInstanceOf(RatesUnavailableException.class);
    }

    @Test
    void treatsSlowResponsesAsFailures() {
        frankfurter.stubFor(get(urlEqualTo("/v1/latest?base=EUR"))
                .willReturn(json(EUR_LATEST).withFixedDelay(1_000)));

        assertThatThrownBy(() -> provider.latest("EUR")).isInstanceOf(RatesUnavailableException.class);
    }

    @Test
    void opensTheCircuitAfterRepeatedFailures() {
        frankfurter.stubFor(get(urlEqualTo("/v1/2024-01-02?base=EUR")).willReturn(serverError()));
        LocalDate date = LocalDate.of(2024, 1, 2);

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> provider.historical("EUR", date)).isInstanceOf(RatesUnavailableException.class);
        }
        assertThat(circuitBreaker().getState()).isEqualTo(CircuitBreaker.State.OPEN);
        frankfurter.resetRequests();

        assertThatThrownBy(() -> provider.historical("EUR", date)).isInstanceOf(RatesUnavailableException.class);
        frankfurter.verify(0, getRequestedFor(urlPathEqualTo("/v1/2024-01-02")));
    }

    @Test
    void unknownCurrenciesAreNeitherRetriedNorCountedAsFailures() {
        frankfurter.stubFor(get(urlEqualTo("/v1/latest?base=ISK"))
                .willReturn(aResponse().withStatus(404).withBody("{\"message\":\"not found\"}")));

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> provider.latest("ISK")).isInstanceOf(UnsupportedCurrencyException.class);
        }

        frankfurter.verify(5, getRequestedFor(urlPathEqualTo("/v1/latest")));
        assertThat(circuitBreaker().getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker().getMetrics().getNumberOfFailedCalls()).isZero();
    }

    private CircuitBreaker circuitBreaker() {
        return circuitBreakers.circuitBreaker(FrankfurterRateProvider.RESILIENCE_INSTANCE);
    }

    private static ResponseDefinitionBuilder json(String body) {
        return aResponse().withHeader("Content-Type", "application/json").withBody(body);
    }
}
