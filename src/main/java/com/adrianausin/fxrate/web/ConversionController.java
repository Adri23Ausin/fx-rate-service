package com.adrianausin.fxrate.web;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.adrianausin.fxrate.rates.ConversionResult;
import com.adrianausin.fxrate.rates.CurrencyConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import org.jspecify.annotations.Nullable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/convert")
@Tag(name = "Conversion", description = "Currency conversion")
class ConversionController {

    private final CurrencyConverter converter;

    ConversionController(CurrencyConverter converter) {
        this.converter = converter;
    }

    @GetMapping
    @Operation(summary = "Convert an amount using the latest rates or the rates of a given date")
    ConversionResult convert(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam @Positive @Digits(integer = 15, fraction = 6) BigDecimal amount,
            @Parameter(description = "Use the rates of this date instead of the latest ones")
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) @Nullable LocalDate date) {
        return converter.convert(from, to, amount, date);
    }
}
