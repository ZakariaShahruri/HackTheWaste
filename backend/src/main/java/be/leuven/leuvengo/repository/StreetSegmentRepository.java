package be.leuven.leuvengo.repository;

import be.leuven.leuvengo.domain.StreetSegment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StreetSegmentRepository extends JpaRepository<StreetSegment, Long> {

    @EntityGraph(attributePaths = "faculty")
    List<StreetSegment> findAllByOrderByNameAsc();
}
