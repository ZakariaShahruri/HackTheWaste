package be.leuven.leuvengo.service;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PredictionTimeTest {

    /** A Wednesday afternoon in Leuven. */
    private static final ZonedDateTime NOW = ZonedDateTime.of(2026, 11, 18, 15, 30, 0, 0, LeuvenTime.ZONE);

    @Test
    void picksTheComingOccurrenceOfThatWeekday() {
        assertThat(PredictionTime.upcoming(DayOfWeek.FRIDAY, 22, NOW))
                .isEqualTo(LocalDateTime.of(2026, 11, 20, 22, 0));
        assertThat(PredictionTime.upcoming(DayOfWeek.MONDAY, 8, NOW))
                .isEqualTo(LocalDateTime.of(2026, 11, 23, 8, 0));
    }

    @Test
    void todayStaysToday() {
        assertThat(PredictionTime.upcoming(DayOfWeek.WEDNESDAY, 9, NOW))
                .isEqualTo(LocalDateTime.of(2026, 11, 18, 9, 0));
    }
}
