package com.adrianausin.fxrate.provider.frankfurter;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Declarative HTTP client for <a href="https://frankfurter.dev">Frankfurter</a>,
 * a free API that publishes the European Central Bank reference rates.
 */
@HttpExchange("/v1")
interface FrankfurterClient {

    @GetExchange("/latest")
    FrankfurterRatesResponse latest(@RequestParam String base);

    @GetExchange("/{date}")
    FrankfurterRatesResponse historical(@PathVariable @DateTimeFormat(iso = ISO.DATE) LocalDate date, @RequestParam String base);
}
