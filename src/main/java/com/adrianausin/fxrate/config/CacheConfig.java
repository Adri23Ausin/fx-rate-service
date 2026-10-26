package com.adrianausin.fxrate.config;

import java.time.Duration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Two caches with different lifetimes: the latest rates change once per business day, while
 * historical rates never change once published.
 * <p>
 * The caching advice runs first (highest precedence) so a cache hit never reaches the
 * circuit breaker, and stale fallback results are never stored.
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching(order = Ordered.HIGHEST_PRECEDENCE)
@EnableConfigurationProperties(CacheConfig.CacheProperties.class)
public class CacheConfig {

    public static final String LATEST_RATES = "latestRates";
    public static final String HISTORICAL_RATES = "historicalRates";

    @Bean
    CacheManager cacheManager(CacheProperties properties) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setAllowNullValues(false);
        cacheManager.registerCustomCache(LATEST_RATES, Caffeine.newBuilder()
                .expireAfterWrite(properties.latestTtl())
                .maximumSize(200)
                .recordStats()
                .build());
        cacheManager.registerCustomCache(HISTORICAL_RATES, Caffeine.newBuilder()
                .expireAfterAccess(properties.historicalTtl())
                .maximumSize(properties.historicalMaxEntries())
                .recordStats()
                .build());
        return cacheManager;
    }

    @ConfigurationProperties("fx.cache")
    record CacheProperties(
            @DefaultValue("10m") Duration latestTtl,
            @DefaultValue("24h") Duration historicalTtl,
            @DefaultValue("10000") long historicalMaxEntries) {
    }
}
