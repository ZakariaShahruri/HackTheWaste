package be.leuven.leuvengo.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DailyTrendTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 18);

    @Test
    void returnsExactlyNDaysOldestFirstZeroFilled() {
        List<DailyTrend.Bucket> buckets = DailyTrend.lastDays(7, List.of(), TODAY, LeuvenTime.ZONE);

        assertThat(buckets).hasSize(7);
        assertThat(buckets.getFirst().date()).isEqualTo(LocalDate.of(2026, 11, 12));
        assertThat(buckets.getLast().date()).isEqualTo(TODAY);
        assertThat(buckets).allSatisfy(b -> assertThat(b.count()).isZero());
        assertThat(buckets.getLast().label()).isEqualTo("11-18");
    }

    @Test
    void bucketsByLeuvenCalendarDayNotUtc() {
        // 23:30 UTC on the 17th is already 00:30 on the 18th in Brussels.
        Instant lateUtc = Instant.parse("2026-11-17T23:30:00Z");

        List<DailyTrend.Bucket> buckets = DailyTrend.lastDays(7, List.of(lateUtc), TODAY, LeuvenTime.ZONE);

        assertThat(buckets.getLast().count()).isEqualTo(1);
        assertThat(buckets.get(5).count()).isZero();
    }

    @Test
    void ignoresEventsOutsideTheWindow() {
        Instant tooOld = Instant.parse("2026-11-01T12:00:00Z");
        assertThat(DailyTrend.lastDays(7, List.of(tooOld), TODAY, LeuvenTime.ZONE))
                .allSatisfy(b -> assertThat(b.count()).isZero());
    }

    @Test
    void keepsChronologicalOrderAcrossNewYear() {
        List<DailyTrend.Bucket> buckets =
                DailyTrend.lastDays(7, List.of(), LocalDate.of(2027, 1, 2), LeuvenTime.ZONE);

        assertThat(buckets).extracting(DailyTrend.Bucket::label)
                .containsExactly("12-27", "12-28", "12-29", "12-30", "12-31", "01-01", "01-02");
    }

    @Test
    void windowStartsAtMidnightInLeuven() {
        assertThat(DailyTrend.windowStart(7, TODAY, LeuvenTime.ZONE))
                .isEqualTo(Instant.parse("2026-11-11T23:00:00Z"));
    }
}
