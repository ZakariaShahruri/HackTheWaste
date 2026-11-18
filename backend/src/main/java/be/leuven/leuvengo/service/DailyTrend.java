package be.leuven.leuvengo.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Counts events per calendar day for the last N days, oldest first, zero-filled. */
public final class DailyTrend {

    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("MM-dd");

    public record Bucket(LocalDate date, String label, long count) {}

    private DailyTrend() {}

    /** Start of the oldest day included in {@link #lastDays}. */
    public static Instant windowStart(int days, LocalDate today, ZoneId zone) {
        return today.minusDays(days - 1L).atStartOfDay(zone).toInstant();
    }

    public static List<Bucket> lastDays(int days, Collection<Instant> events, LocalDate today, ZoneId zone) {
        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        for (int i = days - 1; i >= 0; i--) {
            counts.put(today.minusDays(i), 0L);
        }
        for (Instant event : events) {
            counts.computeIfPresent(event.atZone(zone).toLocalDate(), (d, n) -> n + 1);
        }
        List<Bucket> out = new ArrayList<>(days);
        counts.forEach((date, n) -> out.add(new Bucket(date, date.format(LABEL), n)));
        return out;
    }
}
