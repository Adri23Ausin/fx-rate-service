package com.adrianausin.fxrate.provider.frankfurter;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("fx.provider.frankfurter")
public record FrankfurterProperties(
        @DefaultValue("https://api.frankfurter.dev") String baseUrl,
        @DefaultValue("2s") Duration connectTimeout,
        @DefaultValue("3s") Duration readTimeout) {
}
