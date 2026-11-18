package be.leuven.leuvengo.service;

import be.leuven.leuvengo.domain.Hotspot;
import be.leuven.leuvengo.domain.WorkOrder;
import be.leuven.leuvengo.repository.HotspotRepository;
import be.leuven.leuvengo.repository.WorkOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Planon integration. In production this would POST to Planon's
 * /api/services/{ws}/Order/Create or equivalent endpoint and persist the
 * returned reference. Here we mock the call but keep the shape so swapping in
 * a real {@link org.springframework.web.client.RestClient} is a small change.
 */
@Service
public class PlanonService {

    private static final Logger log = LoggerFactory.getLogger(PlanonService.class);

    private final WorkOrderRepository workOrders;
    private final HotspotRepository hotspots;
    private final Clock clock;

    public PlanonService(WorkOrderRepository workOrders, HotspotRepository hotspots, Clock clock) {
        this.workOrders = workOrders;
        this.hotspots = hotspots;
        this.clock = clock;
    }

    @Transactional
    public WorkOrder dispatch(Hotspot hotspot) {
        return dispatch(hotspot, clock.instant());
    }

    /** Idempotent: if the hotspot is already escalated/dispatched, return existing order. */
    @Transactional
    public WorkOrder dispatch(Hotspot hotspot, Instant at) {
        if (hotspot.getStatus() == Hotspot.Status.DISPATCHED ||
                hotspot.getStatus() == Hotspot.Status.RESOLVED) {
            return workOrders.findAll().stream()
                    .filter(w -> w.getHotspot() != null
                            && w.getHotspot().getId().equals(hotspot.getId()))
                    .findFirst()
                    .orElseGet(() -> createOrder(hotspot, at));
        }
        return createOrder(hotspot, at);
    }

    private WorkOrder createOrder(Hotspot h, Instant at) {
        WorkOrder w = new WorkOrder();
        w.setHotspot(h);
        w.setPriority(priorityFor(h));
        w.setStatus(WorkOrder.Status.PENDING);
        w.setCrew(routeCrew(h));
        w.setSummary(String.format(Locale.ROOT, "Cluster of %d reports - cleanliness %.0f/100 at %s",
                h.getReportCount(),
                h.getMeanCleanliness() == null ? 0 : h.getMeanCleanliness(),
                h.getLabel() == null ? "Leuven" : h.getLabel()));
        w.setCreatedAt(at);
        WorkOrder saved = workOrders.save(w);

        // Mock the actual Planon REST call.
        String ref = callPlanonRest(saved);
        saved.setPlanonRef(ref);
        saved.setStatus(WorkOrder.Status.DISPATCHED);
        saved.setDispatchedAt(at);

        h.setStatus(Hotspot.Status.DISPATCHED);
        h.setLastUpdatedAt(at);
        hotspots.save(h);

        return workOrders.save(saved);
    }

    /**
     * Stand-in for Planon's order API. A real integration would POST the order
     * to Planon with a RestClient and return the reference it hands back.
     */
    private String callPlanonRest(WorkOrder w) {
        log.info("[PLANON-MOCK] dispatching workOrder={} priority={} crew={}",
                w.getId(), w.getPriority(), w.getCrew());
        return "PLN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private WorkOrder.Priority priorityFor(Hotspot h) {
        return priorityFor(
                h.getMeanCleanliness() == null ? 60 : h.getMeanCleanliness(),
                h.getReportCount() == null ? 1 : h.getReportCount());
    }

    /** Dirtier and busier hotspots get a crew sooner. */
    static WorkOrder.Priority priorityFor(double meanCleanliness, int reportCount) {
        if (meanCleanliness <= 20 || reportCount >= 6) return WorkOrder.Priority.URGENT;
        if (meanCleanliness <= 40 || reportCount >= 4) return WorkOrder.Priority.HIGH;
        if (meanCleanliness <= 60) return WorkOrder.Priority.MEDIUM;
        return WorkOrder.Priority.LOW;
    }

    private String routeCrew(Hotspot h) {
        // toy "routing": use street segment district if known
        if (h.getStreetSegment() != null && h.getStreetSegment().getDistrict() != null) {
            return "Crew-" + h.getStreetSegment().getDistrict();
        }
        return "Crew-Centrum";
    }
}
