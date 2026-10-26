package com.adrianausin.fxrate.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

    @Bean
    OpenAPI fxRateOpenApi() {
        return new OpenAPI().info(new Info()
                .title("FX Rate Service")
                .version("v1")
                .description("""
                        Exchange rates and currency conversion based on the European Central Bank \
                        reference rates. Clients are rate limited per `X-API-Key` header (or IP).""")
                .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")));
    }
}
