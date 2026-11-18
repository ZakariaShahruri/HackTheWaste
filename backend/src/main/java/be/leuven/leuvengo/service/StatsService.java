package be.leuven.leuvengo.service;

import be.leuven.leuvengo.domain.Hotspot;
import be.leuven.leuvengo.domain.Report;
import be.leuven.leuvengo.domain.StreetSegment;
import be.leuven.leuvengo.domain.WorkOrder;
import be.leuven.leuvengo.repository.HotspotRepository;
import be.leuven.leuvengo.repository.ReportRepository;
import be.leuven.leuvengo.repository.StreetSegmentRepository;
import be.leuven.leuvengo.repository.WorkOrderRepository;
import be.leuven.leuvengo.web.dto.Dtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Headline numbers for the dashboards. */
@Service
public class StatsService {

    static final int TREND_DAYS = 7;

    private final ReportRepository reports;
    private final HotspotRepository hotspots;
    private final WorkOrderRepository workOrders;
    private final StreetSegmentRepository segments;
    private final Clock clock;

    public StatsService(ReportRepository reports, HotspotRepository hotspots,
                        WorkOrderRepository workOrders, StreetSegmentRepository segments,
                        Clock clock) {
        this.reports = reports;
        this.hotspots = hotspots;
        this.workOrders = workOrders;
        this.segments = segments;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Dtos.StatsDto snapshot() {
        return new Dtos.StatsDto(
                reports.count(),
                hotspots.countByStatusIn(List.of(Hotspot.Status.OPEN, Hotspot.Status.ESCALATED)),
                workOrders.countByStatus(WorkOrder.Status.DISPATCHED),
                workOrders.countByStatus(WorkOrder.Status.COMPLETED),
                averageCleanliness(),
                trend(),
                hotspots.findTop5ByStatusNotOrderByReportCountDesc(Hotspot.Status.RESOLVED)
                        .stream().map(Dtos::of).toList());
    }

    private double averageCleanliness() {
        double avg = segments.findAll().stream()
                .map(StreetSegment::getAiCleanlinessScore)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average().orElse(SegmentScorer.PRIOR_SCORE);
        return Math.round(avg * 10.0) / 10.0;
    }

    private List<Dtos.TimeBucket> trend() {
        LocalDate today = LocalDate.now(clock.withZone(LeuvenTime.ZONE));
        Instant since = DailyTrend.windowStart(TREND_DAYS, today, LeuvenTime.ZONE);
        List<Instant> times = reports.findByReportedAtGreaterThanEqual(since).stream()
                .map(Report::getReportedAt).toList();
        return DailyTrend.lastDays(TREND_DAYS, times, today, LeuvenTime.ZONE).stream()
                .map(b -> new Dtos.TimeBucket(b.date().toString(), b.label(), b.count()))
                .toList();
    }
}
