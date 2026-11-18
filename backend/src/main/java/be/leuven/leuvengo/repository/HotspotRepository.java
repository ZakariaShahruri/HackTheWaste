package be.leuven.leuvengo.repository;

import be.leuven.leuvengo.domain.Hotspot;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface HotspotRepository extends JpaRepository<Hotspot, Long> {

    @EntityGraph(attributePaths = "streetSegment")
    List<Hotspot> findAllByOrderByReportCountDesc();

    @EntityGraph(attributePaths = "streetSegment")
    List<Hotspot> findAllByStatusIn(Collection<Hotspot.Status> statuses);

    @EntityGraph(attributePaths = "streetSegment")
    List<Hotspot> findTop5ByStatusNotOrderByReportCountDesc(Hotspot.Status status);

    long countByStatusIn(Collection<Hotspot.Status> statuses);
}
