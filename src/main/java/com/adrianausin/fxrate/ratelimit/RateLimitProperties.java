package com.adrianausin.fxrate.ratelimit;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param capacity requests a client can make per {@code period}
 * @param period   time it takes to refill the whole capacity
 */
@ConfigurationProperties("fx.rate-limit")
public record RateLimitProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("60") long capacity,
        @DefaultValue("1m") Duration period) {
}
