package be.leuven.leuvengo.service;

import be.leuven.leuvengo.config.LeuvenGoProperties;
import be.leuven.leuvengo.domain.CleanlinessScale;
import be.leuven.leuvengo.domain.Hotspot;
import be.leuven.leuvengo.domain.Report;
import be.leuven.leuvengo.repository.AppUserRepository;
import be.leuven.leuvengo.repository.FacultyRepository;
import be.leuven.leuvengo.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ReportService {

    private final ReportRepository reports;
    private final FacultyRepository faculties;
    private final AppUserRepository users;
    private final TrafficService traffic;
    private final ScoringService scoring;
    private final PlanonService planon;
    private final LeuvenGoProperties props;
    private final Clock clock;

    public ReportService(ReportRepository reports,
                         FacultyRepository faculties,
                         AppUserRepository users,
                         TrafficService traffic,
                         ScoringService scoring,
                         PlanonService planon,
                         LeuvenGoProperties props,
                         Clock clock) {
        this.reports = reports;
        this.faculties = faculties;
        this.users = users;
        this.traffic = traffic;
        this.scoring = scoring;
        this.planon = planon;
        this.props = props;
        this.clock = clock;
    }

    /** A report entering the pipeline. {@code cleanlinessScore} is 0-100 (100 = clean). */
    public record Submission(double lat, double lng, int cleanlinessScore, Report.Source source,
                             Reporter reporter, Evidence evidence) {}

    /** What backs a report up. Everything is optional. */
    public record Evidence(String photoUrl, List<String> tags, String description, String note) {

        public static final Evidence NONE = new Evidence(null, List.of(), null, null);

        public static Evidence tags(List<String> tags) {
            return new Evidence(null, tags, null, null);
        }
    }

    @Transactional
    public Report submit(Submission s) {
        return submit(s, clock.instant());
    }

    /** Files the report as if it came in at {@code reportedAt}, so demo history lines up in time. */
    @Transactional
    public Report submit(Submission s, Instant reportedAt) {
        validate(s);

        Evidence evidence = s.evidence() == null ? Evidence.NONE : s.evidence();
        Report r = new Report();
        r.setLat(s.lat());
        r.setLng(s.lng());
        r.setCleanlinessScore(s.cleanlinessScore());
        r.setSource(s.source());
        r.setPhotoUrl(evidence.photoUrl());
        r.setAiDescription(truncate(evidence.description(), 200));
        r.setNote(truncate(evidence.note(), 500));
        r.setSignalTags(evidence.tags() == null || evidence.tags().isEmpty()
                ? null : truncate(String.join(",", evidence.tags()), 200));
        r.setReportedAt(reportedAt);
        Reporter who = s.reporter();
        r.setReporterPseudoId(who.pseudoId() != null ? who.pseudoId()
                : "anon-" + UUID.randomUUID().toString().substring(0, 8));

        int award = PointsPolicy.pointsFor(s.cleanlinessScore());
        if (who.clan() != null) {
            faculties.findByShortCode(who.clan())
                    .ifPresent(f -> {
                        r.setCreditedFaculty(f);
                        f.setPoints((f.getPoints() == null ? 0 : f.getPoints()) + award);
                    });
        }
        if (who.userId() != null) {
            users.addPoints(who.userId(), award);
        }

        Report saved = reports.save(r);
        Hotspot h = traffic.ingest(saved);

        // Auto-escalate to Planon when threshold crossed.
        int threshold = props.traffic().workOrderThreshold();
        if (h.getStatus() == Hotspot.Status.OPEN
                && h.getReportCount() != null
                && h.getReportCount() >= threshold) {
            h.setStatus(Hotspot.Status.ESCALATED);
            planon.dispatch(h, reportedAt);
        }

        scoring.recomputeNear(saved.getLat(), saved.getLng());
        return saved;
    }

    private void validate(Submission s) {
        if (!LeuvenArea.contains(s.lat(), s.lng())) {
            throw new IllegalArgumentException("Location is outside Leuven");
        }
        if (s.cleanlinessScore() < CleanlinessScale.MIN_SCORE || s.cleanlinessScore() > CleanlinessScale.MAX_SCORE) {
            throw new IllegalArgumentException("Cleanliness score must be 0-100");
        }
        if (s.source() == null) {
            throw new IllegalArgumentException("Source required");
        }
        if (s.reporter() == null) {
            throw new IllegalArgumentException("Reporter required");
        }
    }

    private static String truncate(String v, int max) {
        if (v == null) return null;
        return v.length() <= max ? v : v.substring(0, max);
    }

}
