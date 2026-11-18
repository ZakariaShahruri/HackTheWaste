package be.leuven.leuvengo.service;

import be.leuven.leuvengo.domain.CleanlinessScale;

/**
 * Points earned for a report. Dirtier spots are worth more, because they're
 * the signal the city actually needs; a clean-street check-in still earns 1.
 */
public final class PointsPolicy {

    private PointsPolicy() {}

    public static int pointsFor(int cleanlinessScore) {
        return 1 + (CleanlinessScale.MAX_SCORE - CleanlinessScale.clamp(cleanlinessScore)) / 20;
    }
}
