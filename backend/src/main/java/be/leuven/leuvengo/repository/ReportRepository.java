package be.leuven.leuvengo.repository;

import be.leuven.leuvengo.domain.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByReportedAtGreaterThanEqual(Instant since);

    @Query("""
            select r from Report r
            where r.reportedAt >= :since
              and r.lat between :latMin and :latMax
              and r.lng between :lngMin and :lngMax
            """)
    List<Report> findRecentInBox(Instant since, double latMin, double latMax,
                                 double lngMin, double lngMax);
}
