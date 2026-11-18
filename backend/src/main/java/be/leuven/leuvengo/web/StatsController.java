package be.leuven.leuvengo.web;

import be.leuven.leuvengo.domain.Hotspot;
import be.leuven.leuvengo.domain.Report;
import be.leuven.leuvengo.domain.WorkOrder;
import be.leuven.leuvengo.repository.HotspotRepository;
import be.leuven.leuvengo.repository.ReportRepository;
import be.leuven.leuvengo.repository.StreetSegmentRepository;
import be.leuven.leuvengo.repository.WorkOrderRepository;
import be.leuven.leuvengo.web.dto.Dtos;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final ReportRepository reports;
    private final HotspotRepository hotspots;
    private final WorkOrderRepository workOrders;
    private final StreetSegmentRepository segments;

    public StatsController(ReportRepository reports, HotspotRepository hotspots,
                           WorkOrderRepository workOrders, StreetSegmentRepository segments) {
        this.reports = reports;
        this.hotspots = hotspots;
        this.workOrders = workOrders;
        this.segments = segments;
    }

    @GetMapping
    public Dtos.StatsDto stats() {
        long total = reports.count();
        long open = hotspots.countByStatusIn(
                List.of(Hotspot.Status.OPEN, Hotspot.Status.ESCALATED));
        long dispatched = workOrders.countByStatus(WorkOrder.Status.DISPATCHED);
        long completed = workOrders.countByStatus(WorkOrder.Status.COMPLETED);

        double avg = segments.findAll().stream()
                .mapToDouble(s -> s.getAiCleanlinessScore() == null ? 70 : s.getAiCleanlinessScore())
                .average().orElse(70d);

        return new Dtos.StatsDto(
                total, open, dispatched, completed,
                Math.round(avg * 10.0) / 10.0,
                last7Days(),
                topHotspots());
    }

    private List<Dtos.TimeBucket> last7Days() {
        Instant since = Instant.now().minus(7, ChronoUnit.DAYS);
        Map<String, Long> counts = new TreeMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = 6; i >= 0; i--) {
            String key = LocalDate.now().minusDays(i).format(fmt);
            counts.put(key, 0L);
        }
        for (Report r : reports.findByReportedAtGreaterThanEqual(since)) {
            String key = r.getReportedAt().atZone(ZoneId.systemDefault())
                    .toLocalDate().format(fmt);
            counts.merge(key, 1L, Long::sum);
        }
        List<Dtos.TimeBucket> out = new ArrayList<>();
        counts.forEach((k, v) -> out.add(new Dtos.TimeBucket(k, v)));
        return out;
    }

    private List<Dtos.HotspotDto> topHotspots() {
        return hotspots.findTop5ByStatusNotOrderByReportCountDesc(Hotspot.Status.RESOLVED).stream()
                .map(Dtos::of)
                .toList();
    }
}
