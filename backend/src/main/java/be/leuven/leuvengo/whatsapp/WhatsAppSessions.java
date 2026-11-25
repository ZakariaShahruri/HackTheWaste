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
import java.util.function.Consumer;

/** Remembers the half of a report each sender has sent, until the other half arrives. */
@Service
public class WhatsAppSessions {

    /** A pin or photo older than this is stale; the user has to send it again. */
    public static final Duration TTL = Duration.ofMinutes(30);

    private static final Logger log = LoggerFactory.getLogger(WhatsAppSessions.class);

    /** What we have from a sender so far. */
    public sealed interface Half permits Pin, Photo {}

    public record Pin(double lat, double lng) implements Half {}

    public record Photo(String mediaUrl) implements Half {}

    private final PendingReportRepository pending;
    private final Clock clock;

    public WhatsAppSessions(PendingReportRepository pending, Clock clock) {
        this.pending = pending;
        this.clock = clock;
    }

    /** Keeps this half, replacing whatever the sender sent before. */
    @Transactional
    public void hold(String senderKey, Half half) {
        Instant now = clock.instant();
        Consumer<PendingReport> update = switch (half) {
            case Pin pin -> p -> p.holdPin(pin.lat(), pin.lng(), now);
            case Photo photo -> p -> p.holdPhoto(photo.mediaUrl(), now);
        };
        PendingReport report = pending.findBySenderKey(senderKey).orElseGet(() -> new PendingReport(senderKey));
        update.accept(report);
        pending.save(report);
    }

    /** The sender's own live half, if any. Never another sender's. */
    @Transactional(readOnly = true)
    public Optional<Half> current(String senderKey) {
        Instant cutoff = clock.instant().minus(TTL);
        return pending.findBySenderKey(senderKey)
                .filter(p -> p.getCreatedAt().isAfter(cutoff))
                .map(p -> p.getMediaUrl() != null
                        ? new Photo(p.getMediaUrl())
                        : new Pin(p.getLat(), p.getLng()));
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
