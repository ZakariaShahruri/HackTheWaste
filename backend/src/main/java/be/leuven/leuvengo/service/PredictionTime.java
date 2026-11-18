package be.leuven.leuvengo.service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

/** Turns the heatmap's "weekday + hour" picker into a concrete Leuven local time. */
public final class PredictionTime {

    private PredictionTime() {}

    /** This week's {@code day} at {@code hour}: today if it is that day, otherwise the next one. */
    public static LocalDateTime upcoming(DayOfWeek day, int hour, ZonedDateTime nowInLeuven) {
        return nowInLeuven.toLocalDate()
                .with(TemporalAdjusters.nextOrSame(day))
                .atTime(hour, 0);
    }
}
