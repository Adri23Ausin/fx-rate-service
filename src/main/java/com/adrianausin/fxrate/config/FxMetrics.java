package com.adrianausin.fxrate.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Business metrics, exported to Prometheus with the {@code fx_} prefix.
 */
@Component
public class FxMetrics {

    private final MeterRegistry registry;
    private final Counter rateLimitRejections;

    public FxMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.rateLimitRejections = Counter.builder("fx.rate_limit.rejected")
                .description("Requests rejected because the client exceeded its rate limit")
                .register(registry);
    }

    public void conversion(String from, String to) {
        Counter.builder("fx.conversions")
                .description("Currency conversions performed")
                .tag("from", from)
                .tag("to", to)
                .register(registry)
                .increment();
    }

    /**
     * @param operation {@code latest} or {@code historical}
     * @param outcome   {@code stale} when last known rates were served, {@code unavailable} otherwise
     */
    public void providerFallback(String operation, String outcome) {
        Counter.builder("fx.provider.fallback")
                .description("Calls answered by the fallback because the rate provider failed")
                .tag("operation", operation)
                .tag("outcome", outcome)
                .register(registry)
                .increment();
    }

    public void rateLimitRejected() {
        rateLimitRejections.increment();
    }
}
