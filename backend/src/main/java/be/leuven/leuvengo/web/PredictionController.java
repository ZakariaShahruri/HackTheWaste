package be.leuven.leuvengo.web;

import be.leuven.leuvengo.service.LeuvenTime;
import be.leuven.leuvengo.service.PredictionService;
import be.leuven.leuvengo.service.PredictionTime;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

/** The predictive heatmap. The browser only ever talks to us; the ML service stays internal. */
@RestController
@RequestMapping("/api/predictions")
public class PredictionController {

    private static final int MIN_STEPS = 5;
    private static final int MAX_STEPS = 60;

    private final PredictionService predictions;
    private final Clock clock;

    public PredictionController(PredictionService predictions, Clock clock) {
        this.predictions = predictions;
        this.clock = clock;
    }

    /** {@code available} is false when the ML service is down, so the UI can say so. */
    public record PredictionGrid(LocalDateTime at, boolean available, List<PredictionService.PredictPoint> points) {}

    /**
     * {@code GET /api/predictions/grid?day=4&hour=22}: day 0 = Monday. Both default
     * to the current Leuven day and hour.
     */
    @GetMapping("/grid")
    public PredictionGrid grid(
            @RequestParam(required = false) @Min(0) @Max(6) Integer day,
            @RequestParam(required = false) @Min(0) @Max(23) Integer hour,
            @RequestParam(defaultValue = "30") int steps) {
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(LeuvenTime.ZONE));
        DayOfWeek weekday = day == null ? now.getDayOfWeek() : DayOfWeek.of(day + 1);
        LocalDateTime at = PredictionTime.upcoming(weekday, hour == null ? now.getHour() : hour, now);

        return predictions.predictGrid(at, Math.clamp(steps, MIN_STEPS, MAX_STEPS))
                .map(points -> new PredictionGrid(at, true, points))
                .orElseGet(() -> new PredictionGrid(at, false, List.of()));
    }
}
