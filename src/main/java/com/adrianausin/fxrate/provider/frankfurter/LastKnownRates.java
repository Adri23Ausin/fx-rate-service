package com.adrianausin.fxrate.provider.frankfurter;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.adrianausin.fxrate.rates.RateSnapshot;
import org.springframework.stereotype.Component;

/**
 * Keeps the most recent successful "latest" snapshot per base currency, so the service can
 * keep answering (flagged as stale) while the provider is down. Bounded by the number of
 * currencies the provider supports, so a plain map is enough.
 */
@Component
class LastKnownRates {

    private final Map<String, RateSnapshot> snapshots = new ConcurrentHashMap<>();

    void remember(RateSnapshot snapshot) {
        snapshots.merge(snapshot.base(), snapshot,
                (current, candidate) -> candidate.date().isBefore(current.date()) ? current : candidate);
    }

    Optional<RateSnapshot> latest(String base) {
        return Optional.ofNullable(snapshots.get(base)).map(RateSnapshot::asStale);
    }

    void clear() {
        snapshots.clear();
    }
}
