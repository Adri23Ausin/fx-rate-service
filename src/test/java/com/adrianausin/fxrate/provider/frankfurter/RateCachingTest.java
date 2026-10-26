package com.adrianausin.fxrate.provider.frankfurter;

import java.time.LocalDate;

import com.adrianausin.fxrate.rates.RateService;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
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
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@SpringBootTest
class RateCachingTest {

    @RegisterExtension
    static WireMockExtension frankfurter = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void frankfurterUrl(DynamicPropertyRegistry registry) {
        registry.add("fx.provider.frankfurter.base-url", frankfurter::baseUrl);
    }

    @Autowired
    RateService rateService;

    @Autowired
    CacheManager cacheManager;

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    void latestRatesAreFetchedOncePerBase() {
        frankfurter.stubFor(get(urlEqualTo("/v1/latest?base=EUR")).willReturn(json("""
                {"amount":1.0,"base":"EUR","date":"2026-10-23","rates":{"USD":1.1193}}
                """)));

        rateService.latest("EUR");
        rateService.latest("eur");
        rateService.latest(" Eur ");

        frankfurter.verify(1, getRequestedFor(urlPathEqualTo("/v1/latest")));
    }

    @Test
    void historicalRatesAreCachedPerBaseAndDate() {
        frankfurter.stubFor(get(urlEqualTo("/v1/2024-01-02?base=EUR")).willReturn(json("""
                {"amount":1.0,"base":"EUR","date":"2024-01-02","rates":{"USD":1.0956}}
                """)));
        frankfurter.stubFor(get(urlEqualTo("/v1/2024-01-03?base=EUR")).willReturn(json("""
                {"amount":1.0,"base":"EUR","date":"2024-01-03","rates":{"USD":1.0919}}
                """)));

        rateService.historical("EUR", LocalDate.of(2024, 1, 2));
        rateService.historical("EUR", LocalDate.of(2024, 1, 2));
        rateService.historical("EUR", LocalDate.of(2024, 1, 3));

        frankfurter.verify(1, getRequestedFor(urlPathEqualTo("/v1/2024-01-02")));
        frankfurter.verify(1, getRequestedFor(urlPathEqualTo("/v1/2024-01-03")));
    }

    private static ResponseDefinitionBuilder json(String body) {
        return aResponse().withHeader("Content-Type", "application/json").withBody(body);
    }
}
