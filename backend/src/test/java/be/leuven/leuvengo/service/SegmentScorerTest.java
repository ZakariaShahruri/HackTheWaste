package be.leuven.leuvengo.service;

import be.leuven.leuvengo.service.SegmentScorer.Observation;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SegmentScorerTest {

    private static final Instant NOW = Instant.parse("2026-11-18T12:00:00Z");

    @Test
    void streetWithoutReportsSitsAtThePrior() {
        assertThat(SegmentScorer.score(List.of(), NOW)).isEqualTo(SegmentScorer.PRIOR_SCORE);
    }

    @Test
    void oneFreshFilthyReportPullsTheScoreHalfwayDown() {
        double score = SegmentScorer.score(List.of(new Observation(0, NOW)), NOW);
        assertThat(score).isCloseTo(SegmentScorer.PRIOR_SCORE / 2, within(0.001));
    }

    @Test
    void manyFreshReportsConvergeOnWhatPeopleSee() {
        List<Observation> reports = Collections.nCopies(50, new Observation(20, NOW));
        assertThat(SegmentScorer.score(reports, NOW)).isCloseTo(20, within(2.0));
    }

    @Test
    void olderReportsCountForLess() {
        double fresh = SegmentScorer.score(List.of(new Observation(0, NOW)), NOW);
        double dayOld = SegmentScorer.score(List.of(new Observation(0, NOW.minus(Duration.ofHours(24)))), NOW);
        assertThat(dayOld).isGreaterThan(fresh);
        assertThat(SegmentScorer.recencyWeight(NOW.minus(Duration.ofHours(24)), NOW)).isCloseTo(0.5, within(1e-9));
    }

    @Test
    void cleanReportsRaiseTheScore() {
        assertThat(SegmentScorer.score(List.of(new Observation(100, NOW)), NOW))
                .isGreaterThan(SegmentScorer.PRIOR_SCORE);
    }

    @Test
    void scoreStaysWithinBounds() {
        assertThat(SegmentScorer.score(Collections.nCopies(500, new Observation(0, NOW)), NOW))
                .isBetween(0.0, 100.0);
        assertThat(SegmentScorer.score(Collections.nCopies(500, new Observation(100, NOW)), NOW))
                .isBetween(0.0, 100.0);
    }

    @Test
    void reportsFromTheFutureAreTreatedAsFresh() {
        assertThat(SegmentScorer.recencyWeight(NOW.plusSeconds(60), NOW)).isEqualTo(1.0);
    }
}
