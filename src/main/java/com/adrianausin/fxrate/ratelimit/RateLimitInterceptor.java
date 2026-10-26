package com.adrianausin.fxrate.ratelimit;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Token-bucket rate limiting per client. Clients are identified by their {@code X-API-Key}
 * header, or by IP address when they don't send one.
 * <p>
 * Buckets live in memory, which is fine for a single instance. With several replicas they
 * would move to a shared store (Bucket4j supports Redis and JCache) so limits apply globally.
 */
class RateLimitInterceptor implements HandlerInterceptor {

    static final String API_KEY_HEADER = "X-API-Key";
    static final String LIMIT_HEADER = "X-Rate-Limit-Limit";
    static final String REMAINING_HEADER = "X-Rate-Limit-Remaining";

    private final RateLimitProperties properties;
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .maximumSize(100_000)
            .build();

    RateLimitInterceptor(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Bucket bucket = buckets.get(clientKey(request), key -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        response.setHeader(LIMIT_HEADER, Long.toString(properties.capacity()));
        response.setHeader(REMAINING_HEADER, Long.toString(probe.getRemainingTokens()));
        if (!probe.isConsumed()) {
            long retryAfter = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
            throw new RateLimitExceededException(retryAfter);
        }
        return true;
    }

    private static String clientKey(HttpServletRequest request) {
        String apiKey = request.getHeader(API_KEY_HEADER);
        return apiKey == null || apiKey.isBlank() ? "ip:" + request.getRemoteAddr() : "key:" + apiKey;
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(properties.capacity())
                        .refillGreedy(properties.capacity(), properties.period())
                        .build())
                .build();
    }
}
