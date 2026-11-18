package be.leuven.leuvengo.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;

/**
 * Street cleanliness as a recency-weighted average of report scores, pulled
 * towards a neutral prior so one report can't swing a street to 0 or 100:
 *
 * <pre>score = (prior * w0 + Σ wᵢ * scoreᵢ) / (w0 + Σ wᵢ),   wᵢ = 0.5^(ageᵢ / halfLife)</pre>
 *
 * With no recent reports the street sits at the prior; as fresh reports pile up
 * the score converges to what people are actually seeing.
 */
public final class SegmentScorer {

    /** Assumed score of a street nobody has reported on recently. */
    static final double PRIOR_SCORE = 85;
    /** How many "fresh reports" worth of trust the prior gets. */
    static final double PRIOR_WEIGHT = 1;
    static final Duration HALF_LIFE = Duration.ofHours(24);

    /** One report as seen by the scorer. */
    public record Observation(int score, Instant reportedAt) {}

    private SegmentScorer() {}

    public static double score(Collection<Observation> observations, Instant now) {
        double weightedSum = PRIOR_SCORE * PRIOR_WEIGHT;
        double totalWeight = PRIOR_WEIGHT;
        for (Observation o : observations) {
            double w = recencyWeight(o.reportedAt(), now);
            weightedSum += w * o.score();
            totalWeight += w;
        }
        return weightedSum / totalWeight;
    }

    static double recencyWeight(Instant reportedAt, Instant now) {
        double ageHours = Math.max(0, Duration.between(reportedAt, now).toMinutes() / 60d);
        return Math.pow(0.5, ageHours / HALF_LIFE.toHours());
    }
}
