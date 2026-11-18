package be.leuven.leuvengo.repository;

import be.leuven.leuvengo.domain.WorkOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    @EntityGraph(attributePaths = "hotspot")
    List<WorkOrder> findTop20ByOrderByCreatedAtDesc();

    long countByStatus(WorkOrder.Status status);
}
