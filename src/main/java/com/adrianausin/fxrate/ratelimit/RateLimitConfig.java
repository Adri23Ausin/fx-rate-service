package com.adrianausin.fxrate.ratelimit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RateLimitProperties.class)
@ConditionalOnBooleanProperty(name = "fx.rate-limit.enabled", matchIfMissing = true)
class RateLimitConfig implements WebMvcConfigurer {

    private final RateLimitProperties properties;

    RateLimitConfig(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterceptor(properties)).addPathPatterns("/api/**");
    }
}
