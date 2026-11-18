package be.leuven.leuvengo.service;

import be.leuven.leuvengo.domain.Hotspot;
import be.leuven.leuvengo.domain.WorkOrder;
import be.leuven.leuvengo.repository.HotspotRepository;
import be.leuven.leuvengo.repository.WorkOrderRepository;
import be.leuven.leuvengo.web.dto.Dtos;
import be.leuven.leuvengo.web.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/** The crew-facing side of work orders: listing, completing, manual dispatch. */
@Service
public class WorkOrderService {

    private final WorkOrderRepository workOrders;
    private final HotspotRepository hotspots;
    private final PlanonService planon;
    private final Clock clock;

    public WorkOrderService(WorkOrderRepository workOrders, HotspotRepository hotspots,
                            PlanonService planon, Clock clock) {
        this.workOrders = workOrders;
        this.hotspots = hotspots;
        this.planon = planon;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Dtos.WorkOrderDto> recent() {
        return workOrders.findTop20ByOrderByCreatedAtDesc().stream().map(Dtos::of).toList();
    }

    /** Marks the order done and resolves its hotspot, so new reports there open a fresh one. */
    @Transactional
    public Dtos.WorkOrderDto complete(Long workOrderId) {
        WorkOrder order = workOrders.findById(workOrderId)
                .orElseThrow(() -> new NotFoundException("Work order", workOrderId));
        order.setStatus(WorkOrder.Status.COMPLETED);
        order.setCompletedAt(clock.instant());
        Hotspot hotspot = order.getHotspot();
        hotspot.setStatus(Hotspot.Status.RESOLVED);
        hotspot.setLastUpdatedAt(clock.instant());
        return Dtos.of(order);
    }

    @Transactional
    public Dtos.WorkOrderDto dispatch(Long hotspotId) {
        Hotspot hotspot = hotspots.findById(hotspotId)
                .orElseThrow(() -> new NotFoundException("Hotspot", hotspotId));
        return Dtos.of(planon.dispatch(hotspot));
    }
}
