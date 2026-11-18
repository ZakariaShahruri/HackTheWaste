package be.leuven.leuvengo.service;

import be.leuven.leuvengo.domain.CleanlinessScale;

/**
 * Points earned for a report: 10 for a spotless street up to 60 for a filthy
 * one. Dirtier spots are worth more because they're the signal the city
 * actually needs. The scale is sized against the reward prices, so one or two
 * reports already buy something small.
 */
public final class PointsPolicy {

    private PointsPolicy() {}

    public static int pointsFor(int cleanlinessScore) {
        return 10 + (CleanlinessScale.MAX_SCORE - CleanlinessScale.clamp(cleanlinessScore)) / 2;
    }
}
