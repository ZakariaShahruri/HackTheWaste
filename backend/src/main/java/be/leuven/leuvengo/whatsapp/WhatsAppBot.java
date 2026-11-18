package be.leuven.leuvengo.whatsapp;

import be.leuven.leuvengo.domain.CleanlinessScale;
import be.leuven.leuvengo.domain.TrashPhoto;
import be.leuven.leuvengo.net.SafeHttpFetcher;
import be.leuven.leuvengo.repository.TrashPhotoRepository;
import be.leuven.leuvengo.security.Pseudonyms;
import be.leuven.leuvengo.service.BlobStorageService;
import be.leuven.leuvengo.service.GeminiService;
import be.leuven.leuvengo.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The WhatsApp conversation, independent of Twilio's HTTP details:
 * <ol>
 *   <li>the user shares a location pin, and we remember it for 30 minutes;</li>
 *   <li>the user sends a photo, and we score it, file the report and reply with the verdict.</li>
 * </ol>
 * Phone numbers are only used to derive pseudonyms; they are never stored or logged.
 */
@Service
public class WhatsAppBot {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppBot.class);

    /** One incoming message, as Twilio describes it. Every field may be null. */
    public record Inbound(String from, String body, Double lat, Double lng, String mediaUrl) {

        boolean hasLocation() {
            return lat != null && lng != null;
        }

        boolean hasMedia() {
            return mediaUrl != null && !mediaUrl.isBlank();
        }
    }

    static final String WELCOME = "Hi! Send a 📍 location pin to start a cleanliness report.";
    static final String NEED_LOCATION = "We don't have a location yet. Please share a 📍 location pin first, "
            + "then send the photo again.";
    static final String ASK_FOR_PHOTO = "Got it! 📍 Now please send a photo of the litter "
            + "so our AI can assess it.";
    static final String BAD_MEDIA = "We couldn't open that photo. Please send it again as a normal photo (JPEG or PNG).";

    private final WhatsAppSessions sessions;
    private final Pseudonyms pseudonyms;
    private final TwilioMediaFetcher media;
    private final GeminiService gemini;
    private final BlobStorageService blobStorage;
    private final ReportService reports;
    private final TrashPhotoRepository photos;
    private final Clock clock;

    public WhatsAppBot(WhatsAppSessions sessions, Pseudonyms pseudonyms, TwilioMediaFetcher media,
                       GeminiService gemini, BlobStorageService blobStorage, ReportService reports,
                       TrashPhotoRepository photos, Clock clock) {
        this.sessions = sessions;
        this.pseudonyms = pseudonyms;
        this.media = media;
        this.gemini = gemini;
        this.blobStorage = blobStorage;
        this.reports = reports;
        this.photos = photos;
        this.clock = clock;
    }

    /** Handles one message and returns the text to reply with. */
    public String handle(Inbound message) {
        if (message.from() == null || message.from().isBlank()) {
            return WELCOME;
        }
        String senderKey = pseudonyms.senderKey(message.from());
        String publicId = pseudonyms.publicId(message.from());

        if (message.hasLocation()) {
            log.info("WhatsApp location from {}", publicId);
            sessions.start(senderKey, message.lat(), message.lng());
            return ASK_FOR_PHOTO;
        }
        if (message.hasMedia()) {
            log.info("WhatsApp photo from {}", publicId);
            return handlePhoto(senderKey, publicId, message.mediaUrl());
        }
        return WELCOME;
    }

    private String handlePhoto(String senderKey, String publicId, String mediaUrl) {
        Optional<WhatsAppSessions.Location> location = sessions.current(senderKey);
        if (location.isEmpty()) {
            return NEED_LOCATION;
        }

        SafeHttpFetcher.Download photo;
        try {
            photo = media.fetch(mediaUrl);
        } catch (SafeHttpFetcher.FetchException e) {
            log.warn("Refused WhatsApp media from {}: {}", publicId, e.getMessage());
            return BAD_MEDIA;
        }

        GeminiService.ScoreResult ai = gemini.scorePhoto(photo.bytes(), photo.contentType());
        if (ai.fake()) {
            log.info("Rejected a photo from {} as not genuine: {}", publicId, ai.fakeReason());
            return rejection(ai.fakeReason());
        }

        String photoUrl = blobStorage.upload(photo.bytes(), photo.contentType(), publicId).orElse(null);
        file(location.get(), ai, photoUrl, publicId);
        sessions.finish(senderKey);
        return verdict(ai);
    }

    private void file(WhatsAppSessions.Location at, GeminiService.ScoreResult ai, String photoUrl, String publicId) {
        String tags = ai.tags().isEmpty() ? null : String.join(",", ai.tags());
        reports.submit(new ReportService.Submission(
                at.lat(), at.lng(), ai.score(), "WhatsApp report", photoUrl, tags, null, publicId));

        if (photoUrl != null) {
            TrashPhoto feedItem = new TrashPhoto();
            feedItem.setUsername("WhatsApp user");
            feedItem.setPhotoUrl(photoUrl);
            feedItem.setLat(at.lat());
            feedItem.setLng(at.lng());
            feedItem.setCleanlinessScore(ai.score());
            feedItem.setUserRating(CleanlinessScale.toRating(ai.score()));
            feedItem.setTags(tags);
            feedItem.setReportedAt(clock.instant());
            photos.save(feedItem);
        }
    }

    private static String rejection(String reason) {
        return "🚫 Photo rejected\n\n"
                + "Our AI doesn't think this is a genuine on-site photo: "
                + (reason != null ? reason : "it doesn't look like a real photo taken on location") + ".\n\n"
                + "Please take a real photo of the litter where you're standing and send it again.";
    }

    static String verdict(GeminiService.ScoreResult ai) {
        StringBuilder sb = new StringBuilder();
        if (!ai.aiAvailable()) {
            sb.append("📝 Report received!\n\n");
            sb.append("Our AI inspector is offline right now, so your report was logged with a neutral score.\n");
        } else {
            int score = ai.score();
            String emoji = switch (CleanlinessScale.toRating(score)) {
                case 4, 5 -> "🟢";
                case 3 -> "🟡";
                case 2 -> "🟠";
                default -> "🔴";
            };
            sb.append("🤖 AI analysis complete!\n\n");
            sb.append("Cleanliness score: ").append(score).append("/100 (")
                    .append(CleanlinessScale.label(score)).append(") ").append(emoji).append('\n');
            if (ai.description() != null) {
                sb.append("Assessment: ").append(ai.description()).append('\n');
            }
            if (!ai.tags().isEmpty()) {
                sb.append("Spotted: ")
                        .append(ai.tags().stream().map(t -> "#" + t).collect(Collectors.joining(" ")))
                        .append('\n');
            }
        }
        sb.append("\nYour report is live on the Leuven Go map. Thanks for keeping Leuven clean! 🌿");
        return sb.toString();
    }
}
