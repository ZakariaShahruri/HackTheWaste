package be.leuven.leuvengo.repository;

import be.leuven.leuvengo.domain.PendingReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface PendingReportRepository extends JpaRepository<PendingReport, Long> {

    Optional<PendingReport> findBySenderKey(String senderKey);

    @Modifying
    @Query("delete from PendingReport p where p.createdAt < :cutoff")
    int deleteCreatedBefore(Instant cutoff);
}
