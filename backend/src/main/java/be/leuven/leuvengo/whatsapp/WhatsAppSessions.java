package be.leuven.leuvengo.whatsapp;

import be.leuven.leuvengo.domain.PendingReport;
import be.leuven.leuvengo.repository.PendingReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/** Remembers each sender's location pin until their photo arrives. */
@Service
public class WhatsAppSessions {

    /** A pin older than this is stale; the user has to share their location again. */
    public static final Duration TTL = Duration.ofMinutes(30);

    private static final Logger log = LoggerFactory.getLogger(WhatsAppSessions.class);

    public record Location(double lat, double lng) {}

    private final PendingReportRepository pending;
    private final Clock clock;

    public WhatsAppSessions(PendingReportRepository pending, Clock clock) {
        this.pending = pending;
        this.clock = clock;
    }

    /** Starts (or restarts) a report at this location. */
    @Transactional
    public void start(String senderKey, double lat, double lng) {
        Instant now = clock.instant();
        pending.findBySenderKey(senderKey).ifPresentOrElse(
                p -> p.moveTo(lat, lng, now),
                () -> pending.save(new PendingReport(senderKey, lat, lng, now)));
    }

    /** The sender's own live pin, if any. Never another sender's. */
    @Transactional(readOnly = true)
    public Optional<Location> current(String senderKey) {
        Instant cutoff = clock.instant().minus(TTL);
        return pending.findBySenderKey(senderKey)
                .filter(p -> p.getCreatedAt().isAfter(cutoff))
                .map(p -> new Location(p.getLat(), p.getLng()));
    }

    @Transactional
    public void finish(String senderKey) {
        pending.findBySenderKey(senderKey).ifPresent(pending::delete);
    }

    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
    @Transactional
    public void purgeExpired() {
        int removed = pending.deleteCreatedBefore(clock.instant().minus(TTL));
        if (removed > 0) log.info("Expired {} unfinished WhatsApp reports", removed);
    }
}
