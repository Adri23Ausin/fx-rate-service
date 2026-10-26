package com.adrianausin.fxrate.ratelimit;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import com.adrianausin.fxrate.rates.RateService;
import com.adrianausin.fxrate.rates.RateSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@SpringBootTest(properties = {"fx.rate-limit.capacity=3", "fx.rate-limit.period=1h"})
@AutoConfigureMockMvc
class RateLimitTest {

    @Autowired
    MockMvcTester mvc;

    @MockitoBean
    RateService rateService;

    @BeforeEach
    void stubRates() {
        given(rateService.latest(anyString())).willReturn(new RateSnapshot("EUR", LocalDate.of(2026, 10, 23),
                Map.of("USD", BigDecimal.ONE), Instant.EPOCH, false));
    }

    @Test
    void rejectsRequestsOverTheLimitWithRetryAfter() {
        for (int remaining = 2; remaining >= 0; remaining--) {
            assertThat(mvc.get().uri("/api/v1/rates/latest").header("X-API-Key", "client-a"))
                    .hasStatusOk()
                    .hasHeader("X-Rate-Limit-Remaining", Integer.toString(remaining));
        }

        assertThat(mvc.get().uri("/api/v1/rates/latest").header("X-API-Key", "client-a"))
                .hasStatus(HttpStatus.TOO_MANY_REQUESTS)
                .containsHeader("Retry-After")
                .bodyJson()
                .extractingPath("$.status").isEqualTo(429);
    }

    @Test
    void limitsEachClientIndependently() {
        for (int i = 0; i < 3; i++) {
            mvc.get().uri("/api/v1/rates/latest").header("X-API-Key", "client-b").exchange();
        }

        assertThat(mvc.get().uri("/api/v1/rates/latest").header("X-API-Key", "client-c"))
                .hasStatusOk();
    }

    @Test
    void doesNotLimitNonApiEndpoints() {
        for (int i = 0; i < 5; i++) {
            assertThat(mvc.get().uri("/v3/api-docs")).hasStatusOk();
        }
    }
}
