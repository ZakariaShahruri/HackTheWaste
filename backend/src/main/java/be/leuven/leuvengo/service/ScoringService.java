package be.leuven.leuvengo.service;

import be.leuven.leuvengo.config.LeuvenGoProperties;
import be.leuven.leuvengo.domain.Report;
import be.leuven.leuvengo.domain.StreetSegment;
import be.leuven.leuvengo.repository.ReportRepository;
import be.leuven.leuvengo.repository.StreetSegmentRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Keeps each street segment's 0-100 cleanliness score up to date from the
 * reports around it (see {@link SegmentScorer} for the formula).
 */
@Service
public class ScoringService {

    /** Reports within this distance of a segment's centre count towards it. */
    static final double SEGMENT_RADIUS_M = 80;

    private final ReportRepository reports;
    private final StreetSegmentRepository segments;
    private final LeuvenGoProperties props;
    private final Clock clock;

    public ScoringService(ReportRepository reports,
                          StreetSegmentRepository segments,
                          LeuvenGoProperties props,
                          Clock clock) {
        this.reports = reports;
        this.segments = segments;
        this.props = props;
        this.clock = clock;
    }

    /** Re-scores only the segments a report at this point can influence. */
    @Transactional
    public void recomputeNear(double lat, double lng) {
        Instant now = clock.instant();
        for (StreetSegment seg : segments.findAll()) {
            if (hasCentre(seg)
                    && GeoUtil.distanceMeters(lat, lng, seg.getCenterLat(), seg.getCenterLng()) <= SEGMENT_RADIUS_M) {
                GeoBox box = GeoBox.around(seg.getCenterLat(), seg.getCenterLng(), SEGMENT_RADIUS_M);
                List<Report> nearby = reports.findRecentInBox(windowStart(now),
                        box.latMin(), box.latMax(), box.lngMin(), box.lngMax());
                rescore(seg, nearby, now);
            }
        }
    }

    /**
     * Re-scores every segment. Runs hourly so scores relax back towards the
     * prior as reports age, even when no new reports come in.
     */
    @Scheduled(cron = "${leuvengo.scoring.refresh-cron:0 0 * * * *}")
    @Transactional
    public void recomputeAll() {
        Instant now = clock.instant();
        List<Report> recent = reports.findByReportedAtGreaterThanEqual(windowStart(now));
        for (StreetSegment seg : segments.findAll()) {
            if (hasCentre(seg)) rescore(seg, recent, now);
        }
    }

    private void rescore(StreetSegment seg, List<Report> candidates, Instant now) {
        List<SegmentScorer.Observation> observations = candidates.stream()
                .filter(r -> GeoUtil.distanceMeters(seg.getCenterLat(), seg.getCenterLng(),
                        r.getLat(), r.getLng()) <= SEGMENT_RADIUS_M)
                .map(r -> new SegmentScorer.Observation(r.getCleanlinessScore(), r.getReportedAt()))
                .toList();
        seg.setAiCleanlinessScore(round1(SegmentScorer.score(observations, now)));
        seg.setRecentReportCount(observations.size());
        seg.setLastEvaluatedAt(now);
    }

    private Instant windowStart(Instant now) {
        return now.minus(props.traffic().reportTtlHours(), ChronoUnit.HOURS);
    }

    private static boolean hasCentre(StreetSegment seg) {
        return seg.getCenterLat() != null && seg.getCenterLng() != null;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
