package be.leuven.leuvengo.demo;

import be.leuven.leuvengo.service.GeoUtil;
import be.leuven.leuvengo.service.LeuvenArea;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class DemoActivityTest {

    private static final Instant NOW = Instant.parse("2026-11-18T12:00:00Z");
    private static final List<DemoActivity.Event> WEEK = DemoActivity.plan(NOW, new Random(DemoActivitySeeder.SEED));

    @Test
    void theSameSeedGivesTheSameWeek() {
        assertThat(DemoActivity.plan(NOW, new Random(DemoActivitySeeder.SEED))).isEqualTo(WEEK);
    }

    @Test
    void everythingHappensInThePastWeekOldestFirst() {
        assertThat(WEEK).isSortedAccordingTo(Comparator.comparing(DemoActivity.Event::at));
        assertThat(WEEK).allSatisfy(e -> assertThat(e.at())
                .isBefore(NOW)
                .isAfter(NOW.minus(DemoActivity.SPAN)));
    }

    @Test
    void reportsAtASpotStayWithinTheHotspotRadiusOfIt() {
        assertThat(reports()).allSatisfy(r -> {
            assertThat(LeuvenArea.contains(r.lat(), r.lng())).isTrue();
            if (r.spot() != null) {
                DemoCity.Street street = DemoCity.street(r.spot());
                assertThat(GeoUtil.distanceMeters(r.lat(), r.lng(), street.lat(), street.lng())).isLessThan(40);
            }
        });
    }

    @Test
    void spotsAreOnlyCleanedUpAfterTheirLastReport() {
        List<DemoActivity.Cleanup> cleanups = WEEK.stream()
                .filter(DemoActivity.Cleanup.class::isInstance).map(DemoActivity.Cleanup.class::cast).toList();

        assertThat(cleanups).isNotEmpty().allSatisfy(cleanup -> assertThat(reports())
                .filteredOn(r -> cleanup.spot().equals(r.spot()))
                .hasSizeGreaterThanOrEqualTo(3)
                .allSatisfy(r -> assertThat(r.at()).isBefore(cleanup.at())));
    }

    private static List<DemoActivity.Report> reports() {
        return WEEK.stream()
                .filter(DemoActivity.Report.class::isInstance).map(DemoActivity.Report.class::cast).toList();
    }
}
