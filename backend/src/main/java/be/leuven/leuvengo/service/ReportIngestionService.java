package be.leuven.leuvengo.service;

import be.leuven.leuvengo.domain.Report;
import be.leuven.leuvengo.photo.ImageNormalizer;
import be.leuven.leuvengo.photo.PhotoStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Turns a photo taken somewhere in Leuven into a filed report, whichever
 * channel it came from (WhatsApp, the browser demo). Every photo report goes
 * through here, so they are all checked, scored and stored the same way.
 */
@Service
public class ReportIngestionService {

    private static final Logger log = LoggerFactory.getLogger(ReportIngestionService.class);

    /** A photo report as received. */
    public record PhotoReport(double lat, double lng, byte[] image, Report.Source source, Reporter reporter) {}

    public sealed interface Outcome permits Filed, Rejected {}

    /** The report is in: on the map, in the feed, and counted towards hotspots. */
    public record Filed(long reportId, long hotspotId, String photoUrl,
                        GeminiService.ScoreResult verdict) implements Outcome {}

    public record Rejected(Reason reason, String detail) implements Outcome {}

    public enum Reason { OUTSIDE_LEUVEN, UNREADABLE_IMAGE, NOT_GENUINE }

    private final ImageNormalizer images;
    private final GeminiService gemini;
    private final PhotoStorage storage;
    private final ReportService reports;
    private final TransactionTemplate transaction;

    public ReportIngestionService(ImageNormalizer images, GeminiService gemini, PhotoStorage storage,
                                  ReportService reports, TransactionTemplate transaction) {
        this.images = images;
        this.gemini = gemini;
        this.storage = storage;
        this.reports = reports;
        this.transaction = transaction;
    }

    public Outcome ingest(PhotoReport in) {
        if (!LeuvenArea.contains(in.lat(), in.lng())) {
            return new Rejected(Reason.OUTSIDE_LEUVEN, null);
        }

        ImageNormalizer.Normalized photo;
        try {
            photo = images.normalize(in.image());
        } catch (ImageNormalizer.UnreadableImageException e) {
            return new Rejected(Reason.UNREADABLE_IMAGE, e.getMessage());
        }

        // Scored outside the transaction: it's a slow network call.
        GeminiService.ScoreResult verdict = gemini.scorePhoto(photo.bytes(), photo.contentType());
        if (verdict.fake()) {
            log.info("Rejected a {} photo from {} as not genuine", in.source(), in.reporter().pseudoId());
            return new Rejected(Reason.NOT_GENUINE, verdict.fakeReason());
        }

        // Photo and report are saved together, so there's never a feed item without a report.
        return transaction.execute(status -> {
            String photoUrl = storage.store(photo, in.reporter().pseudoId());
            Report report = reports.submit(new ReportService.Submission(
                    in.lat(), in.lng(), verdict.score(), in.source(), in.reporter(),
                    new ReportService.Evidence(photoUrl, verdict.tags(), verdict.description(), null)));
            return new Filed(report.getId(), report.getHotspot().getId(), photoUrl, verdict);
        });
    }
}
