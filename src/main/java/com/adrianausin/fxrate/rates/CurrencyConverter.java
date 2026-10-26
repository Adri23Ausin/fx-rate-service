package com.adrianausin.fxrate.rates;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Currency;

import com.adrianausin.fxrate.config.FxMetrics;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * Converts amounts between currencies using the reference rates.
 * <p>
 * The result is rounded to the minor unit of the target currency (2 decimals for EUR,
 * 0 for JPY, 3 for KWD...) with {@link RoundingMode#HALF_EVEN}, the "banker's rounding"
 * that avoids an upward bias when many amounts are rounded.
 */
@Service
public class CurrencyConverter {

    private final RateService rateService;
    private final FxMetrics metrics;

    public CurrencyConverter(RateService rateService, FxMetrics metrics) {
        this.rateService = rateService;
        this.metrics = metrics;
    }

    public ConversionResult convert(String from, String to, BigDecimal amount, @Nullable LocalDate date) {
        String source = CurrencyCodes.normalize(from);
        String target = CurrencyCodes.normalize(to);

        RateSnapshot snapshot = date == null
                ? rateService.latest(source)
                : rateService.historical(source, date);
        BigDecimal rate = snapshot.rateFor(target).orElseThrow(() -> new UnsupportedCurrencyException(target));

        BigDecimal result = amount.multiply(rate).setScale(minorUnits(target), RoundingMode.HALF_EVEN);
        metrics.conversion(source, target);
        return new ConversionResult(source, target, amount, result, rate, snapshot.date(), snapshot.stale());
    }

    private static int minorUnits(String currency) {
        // Precious metals and other non-cash codes report -1 fraction digits
        return Math.max(Currency.getInstance(currency).getDefaultFractionDigits(), 0);
    }
}
