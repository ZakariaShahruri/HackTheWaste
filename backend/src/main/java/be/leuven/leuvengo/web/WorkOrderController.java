package be.leuven.leuvengo.web;

import be.leuven.leuvengo.domain.AppUser;
import be.leuven.leuvengo.domain.Role;
import be.leuven.leuvengo.security.CurrentUser;
import be.leuven.leuvengo.service.WorkOrderService;
import be.leuven.leuvengo.web.dto.Dtos;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrders;

    public WorkOrderController(WorkOrderService workOrders) {
        this.workOrders = workOrders;
    }

    @GetMapping
    public List<Dtos.WorkOrderDto> list() {
        return workOrders.recent();
    }

    /** Crew only; the parameter enforces the role check. */
    @PostMapping("/{id}/complete")
    public Dtos.WorkOrderDto complete(@CurrentUser(role = Role.CREW) AppUser crew, @PathVariable Long id) {
        return workOrders.complete(id);
    }

    @PostMapping("/dispatch/{hotspotId}")
    public Dtos.WorkOrderDto dispatch(@CurrentUser(role = Role.CREW) AppUser crew, @PathVariable Long hotspotId) {
        return workOrders.dispatch(hotspotId);
    }
}
