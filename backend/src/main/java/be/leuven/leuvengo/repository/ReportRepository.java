package be.leuven.leuvengo.repository;

import be.leuven.leuvengo.domain.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByReportedAtGreaterThanEqual(Instant since);

    @Query("select r.photoUrl from Report r where r.photoUrl is not null")
    List<String> findAllPhotoUrls();

    /** The public feed: photo reports, newest first. */
    @EntityGraph(attributePaths = {"creditedFaculty", "hotspot.streetSegment"})
    Page<Report> findByPhotoUrlIsNotNullOrderByReportedAtDesc(Pageable page);

    @EntityGraph(attributePaths = {"creditedFaculty", "hotspot.streetSegment"})
    Page<Report> findByPhotoUrlIsNotNullAndCreditedFacultyShortCodeOrderByReportedAtDesc(
            String facultyShortCode, Pageable page);

    @Query("""
            select r from Report r
            where r.reportedAt >= :since
              and r.lat between :latMin and :latMax
              and r.lng between :lngMin and :lngMax
            """)
    List<Report> findRecentInBox(Instant since, double latMin, double latMax,
                                 double lngMin, double lngMax);
}
