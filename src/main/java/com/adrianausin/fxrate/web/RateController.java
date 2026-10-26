package com.adrianausin.fxrate.web;

import java.time.LocalDate;
import java.util.Set;

import com.adrianausin.fxrate.rates.CurrencyCodes;
import com.adrianausin.fxrate.rates.RateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rates")
@Tag(name = "Rates", description = "ECB reference exchange rates")
class RateController {

    private final RateService rateService;

    RateController(RateService rateService) {
        this.rateService = rateService;
    }

    @GetMapping("/latest")
    @Operation(summary = "Latest published rates for a base currency")
    RatesResponse latest(
            @RequestParam(defaultValue = "EUR") String base,
            @Parameter(description = "Comma-separated currencies to return, all when omitted")
            @RequestParam(defaultValue = "") Set<String> symbols) {
        return RatesResponse.from(rateService.latest(base).only(CurrencyCodes.normalize(symbols)));
    }

    @GetMapping("/{date}")
    @Operation(summary = "Rates published on a given date (the previous business day on weekends)")
    RatesResponse historical(
            @PathVariable @DateTimeFormat(iso = ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "EUR") String base,
            @Parameter(description = "Comma-separated currencies to return, all when omitted")
            @RequestParam(defaultValue = "") Set<String> symbols) {
        return RatesResponse.from(rateService.historical(base, date).only(CurrencyCodes.normalize(symbols)));
    }
}
