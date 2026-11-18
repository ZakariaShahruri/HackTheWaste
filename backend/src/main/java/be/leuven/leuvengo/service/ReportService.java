package be.leuven.leuvengo.service;

import be.leuven.leuvengo.config.LeuvenGoProperties;
import be.leuven.leuvengo.domain.CleanlinessScale;
import be.leuven.leuvengo.domain.Hotspot;
import be.leuven.leuvengo.domain.Report;
import be.leuven.leuvengo.repository.FacultyRepository;
import be.leuven.leuvengo.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ReportService {

    private final ReportRepository reports;
    private final FacultyRepository faculties;
    private final TrafficService traffic;
    private final ScoringService scoring;
    private final PlanonService planon;
    private final LeuvenGoProperties props;

    public ReportService(ReportRepository reports,
                         FacultyRepository faculties,
                         TrafficService traffic,
                         ScoringService scoring,
                         PlanonService planon,
                         LeuvenGoProperties props) {
        this.reports = reports;
        this.faculties = faculties;
        this.traffic = traffic;
        this.scoring = scoring;
        this.planon = planon;
        this.props = props;
    }

    /** A report entering the pipeline; {@code cleanlinessScore} is 0-100 (100 = clean). */
    public record Submission(Double lat, Double lng, Integer cleanlinessScore,
                             String note, String imageRef,
                             String signalTags, String facultyShortCode,
                             String reporterPseudoId) {}

    @Transactional
    public Report submit(Submission s) {
        validate(s);

        Report r = new Report();
        r.setLat(s.lat());
        r.setLng(s.lng());
        r.setCleanlinessScore(s.cleanlinessScore());
        r.setNote(safe(s.note(), 500));
        r.setImageRef(s.imageRef());
        r.setSignalTags(safe(s.signalTags(), 200));
        r.setReportedAt(Instant.now());
        r.setReporterPseudoId(s.reporterPseudoId() != null ? s.reporterPseudoId()
                : "anon-" + UUID.randomUUID().toString().substring(0, 8));

        if (s.facultyShortCode() != null) {
            faculties.findByShortCode(s.facultyShortCode())
                    .ifPresent(f -> {
                        r.setCrediteFaculty(f);
                        int award = PointsPolicy.pointsFor(s.cleanlinessScore());
                        f.setPoints((f.getPoints() == null ? 0 : f.getPoints()) + award);
                    });
        }

        Report saved = reports.save(r);
        Hotspot h = traffic.ingest(saved);

        // Auto-escalate to Planon when threshold crossed.
        int threshold = props.traffic().workOrderThreshold();
        if (h.getStatus() == Hotspot.Status.OPEN
                && h.getReportCount() != null
                && h.getReportCount() >= threshold) {
            h.setStatus(Hotspot.Status.ESCALATED);
            planon.dispatch(h);
        }

        scoring.recomputeNear(saved.getLat(), saved.getLng());
        return saved;
    }

    private void validate(Submission s) {
        if (s.lat() == null || s.lng() == null) {
            throw new IllegalArgumentException("Coordinates required");
        }
        if (s.cleanlinessScore() == null
                || s.cleanlinessScore() < CleanlinessScale.MIN_SCORE
                || s.cleanlinessScore() > CleanlinessScale.MAX_SCORE) {
            throw new IllegalArgumentException("Cleanliness score must be 0-100");
        }
    }

    private String safe(String v, int max) {
        if (v == null) return null;
        return v.length() <= max ? v : v.substring(0, max);
    }

}
