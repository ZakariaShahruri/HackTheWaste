package be.leuven.leuvengo.repository;

import be.leuven.leuvengo.domain.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {
    Optional<Faculty> findByShortCode(String shortCode);

    List<Faculty> findAllByOrderByShortCodeAsc();

    long countByPointsGreaterThan(int points);
}
