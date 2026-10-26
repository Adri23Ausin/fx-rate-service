package com.adrianausin.fxrate.rates;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConversionResult(
        String from,
        String to,
        BigDecimal amount,
        BigDecimal result,
        BigDecimal rate,
        LocalDate rateDate,
        boolean stale) {
}
