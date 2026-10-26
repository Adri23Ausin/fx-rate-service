package com.adrianausin.fxrate.web;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.adrianausin.fxrate.rates.ConversionResult;
import com.adrianausin.fxrate.rates.CurrencyConverter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@WebMvcTest(controllers = ConversionController.class, properties = "fx.rate-limit.enabled=false")
class ConversionControllerTest {

    @Autowired
    MockMvcTester mvc;

    @MockitoBean
    CurrencyConverter converter;

    @Test
    void convertsAnAmount() {
        LocalDate date = LocalDate.of(2026, 10, 23);
        given(converter.convert("EUR", "USD", new BigDecimal("100"), null)).willReturn(new ConversionResult(
                "EUR", "USD", new BigDecimal("100"), new BigDecimal("111.93"), new BigDecimal("1.1193"), date, false));

        assertThat(mvc.get().uri("/api/v1/convert?from=EUR&to=USD&amount=100"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.result").isEqualTo(111.93);
    }

    @Test
    void rejectsNegativeAmounts() {
        assertThat(mvc.get().uri("/api/v1/convert?from=EUR&to=USD&amount=-5"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsAmountsWithTooManyDecimals() {
        assertThat(mvc.get().uri("/api/v1/convert?from=EUR&to=USD&amount=1.1234567"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void requiresTheTargetCurrency() {
        assertThat(mvc.get().uri("/api/v1/convert?from=EUR&amount=10"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }
}
