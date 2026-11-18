package be.leuven.leuvengo.demo;

import be.leuven.leuvengo.domain.Report;
import be.leuven.leuvengo.service.ReportService;
import be.leuven.leuvengo.service.Reporter;
import be.leuven.leuvengo.service.ScoringService;
import be.leuven.leuvengo.service.WorkOrderService;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Plays a {@link DemoActivity} week through the real report pipeline, so the
 * hotspots, work orders, street scores and clan points all follow from it
 * exactly as they would from real reports.
 */
@Component
class DemoActivitySeeder {

    static final long SEED = 42;

    private final ReportService reports;
    private final WorkOrderService workOrders;
    private final ScoringService scoring;
    private final Clock clock;

    DemoActivitySeeder(ReportService reports, WorkOrderService workOrders, ScoringService scoring, Clock clock) {
        this.reports = reports;
        this.workOrders = workOrders;
        this.scoring = scoring;
        this.clock = clock;
    }

    /** Expects to run inside a transaction, on a city without reports. */
    void seed() {
        Map<String, Long> hotspotBySpot = new HashMap<>();
        for (DemoActivity.Event event : DemoActivity.plan(clock.instant(), new Random(SEED))) {
            switch (event) {
                case DemoActivity.Report r -> {
                    Report filed = reports.submit(submission(r), r.at());
                    if (r.spot() != null) hotspotBySpot.put(r.spot(), filed.getHotspot().getId());
                }
                case DemoActivity.Cleanup c -> workOrders.completeAt(hotspotBySpot.get(c.spot()), c.at());
            }
        }
        // Report-time scoring used the real "now"; one pass at the end settles every street.
        scoring.recomputeAll();
    }

    private static ReportService.Submission submission(DemoActivity.Report r) {
        return new ReportService.Submission(r.lat(), r.lng(), r.score(), Report.Source.DEMO,
                Reporter.anonymous(r.reporterId(), r.clan()), ReportService.Evidence.tags(r.tags()));
    }
}
