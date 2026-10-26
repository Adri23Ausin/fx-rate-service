package com.adrianausin.fxrate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureMetrics
class ObservabilityTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void exposesPrometheusMetrics() {
        assertThat(mvc.get().uri("/actuator/prometheus"))
                .hasStatusOk()
                .bodyText()
                .contains("resilience4j_circuitbreaker_state")
                .contains("cache_gets_total")
                .contains("fx_rate_limit_rejected_total")
                .contains("application=\"fx-rate-service\"");
    }

    @Test
    void reportsCircuitBreakerHealth() {
        assertThat(mvc.get().uri("/actuator/health"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.components.circuitBreakers.details.frankfurter.status").isEqualTo("UP");
    }
}
