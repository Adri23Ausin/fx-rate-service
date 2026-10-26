package com.adrianausin.fxrate.ratelimit;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * Rendered as a 429 problem detail by Spring MVC, including the {@code Retry-After} header.
 */
class RateLimitExceededException extends ErrorResponseException {

    private final long retryAfterSeconds;

    RateLimitExceededException(long retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, problem(retryAfterSeconds), null);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    @Override
    public HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
        return headers;
    }

    private static ProblemDetail problem(long retryAfterSeconds) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
                "Rate limit exceeded, retry in %d seconds".formatted(retryAfterSeconds));
    }
}
