package be.leuven.leuvengo.demo;

import be.leuven.leuvengo.config.LeuvenGoProperties;
import be.leuven.leuvengo.domain.Report;
import be.leuven.leuvengo.photo.PhotoStorage;
import be.leuven.leuvengo.repository.FacultyRepository;
import be.leuven.leuvengo.repository.HotspotRepository;
import be.leuven.leuvengo.repository.ReportRepository;
import be.leuven.leuvengo.repository.WorkOrderRepository;
import be.leuven.leuvengo.service.LeuvenTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

/**
 * Winds a demo instance back to a fresh week every night: public test uploads
 * don't pile up, and the seeded history never ages past the 72-hour scoring
 * window. Accounts, their points and redeemed vouchers are kept. Hosts that
 * sleep when idle (Cloud Run) miss the nightly run, so a start catches up
 * when the week is more than {@link #MAX_AGE} old.
 */
@Component
class DemoReset {

    private static final Logger log = LoggerFactory.getLogger(DemoReset.class);
    static final Duration MAX_AGE = Duration.ofDays(1);

    private final ReportRepository reports;
    private final HotspotRepository hotspots;
    private final WorkOrderRepository workOrders;
    private final FacultyRepository faculties;
    private final PhotoStorage photos;
    private final DemoActivitySeeder activity;
    private final LeuvenGoProperties props;
    private final TransactionTemplate transaction;
    private final Clock clock;

    DemoReset(ReportRepository reports, HotspotRepository hotspots, WorkOrderRepository workOrders,
              FacultyRepository faculties, PhotoStorage photos, DemoActivitySeeder activity,
              LeuvenGoProperties props, TransactionTemplate transaction, Clock clock) {
        this.reports = reports;
        this.hotspots = hotspots;
        this.workOrders = workOrders;
        this.faculties = faculties;
        this.photos = photos;
        this.activity = activity;
        this.props = props;
        this.transaction = transaction;
        this.clock = clock;
    }

    @Scheduled(cron = "${leuvengo.demo.reset-cron:0 0 4 * * *}", zone = LeuvenTime.ZONE_ID)
    void nightly() {
        if (props.demo().enabled()) reset();
    }

    boolean isStale() {
        return reports.findLatestReportedAt(Report.Source.DEMO)
                .map(latest -> latest.isBefore(clock.instant().minus(MAX_AGE)))
                .orElse(false);
    }

    void reset() {
        transaction.executeWithoutResult(status -> {
            List<String> photoUrls = reports.findAllPhotoUrls();
            workOrders.deleteAllInBatch();
            reports.deleteAllInBatch();
            hotspots.deleteAllInBatch();
            photoUrls.forEach(photos::delete);
            faculties.findAll().forEach(f -> f.setPoints(0));
            activity.seed();
        });
        log.info("Reset the demo to a fresh week: {} reports, {} work orders", reports.count(), workOrders.count());
    }
}
