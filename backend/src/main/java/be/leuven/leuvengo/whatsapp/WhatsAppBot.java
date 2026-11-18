package be.leuven.leuvengo.whatsapp;

import be.leuven.leuvengo.domain.CleanlinessScale;
import be.leuven.leuvengo.domain.Faculty;
import be.leuven.leuvengo.domain.TrashPhoto;
import be.leuven.leuvengo.net.SafeHttpFetcher;
import be.leuven.leuvengo.photo.ImageNormalizer;
import be.leuven.leuvengo.photo.PhotoStorage;
import be.leuven.leuvengo.repository.TrashPhotoRepository;
import be.leuven.leuvengo.security.Pseudonyms;
import be.leuven.leuvengo.service.GeminiService;
import be.leuven.leuvengo.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The WhatsApp conversation, independent of Twilio's HTTP details:
 * <ol>
 *   <li>the user shares a location pin, and we remember it for 30 minutes;</li>
 *   <li>the user sends a photo, and we score it, file the report and reply with the verdict.</li>
 * </ol>
 * Sending {@code clan CODE} links the sender to a faculty, which then earns points for their reports.
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

    static final String WELCOME = "Hi! Send a 📍 location pin to start a cleanliness report.\n\n"
            + "Reply \"clan\" to pick the faculty your reports earn points for.";
    private static final Pattern CLAN_COMMAND = Pattern.compile("(?i)\\s*clan\\b\\s*(\\S*)\\s*");
    static final String NEED_LOCATION = "We don't have a location yet. Please share a 📍 location pin first, "
            + "then send the photo again.";
    static final String ASK_FOR_PHOTO = "Got it! 📍 Now please send a photo of the litter "
            + "so our AI can assess it.";
    static final String BAD_MEDIA = "We couldn't open that photo. Please send it again as a normal photo.";

    private final WhatsAppSessions sessions;
    private final Pseudonyms pseudonyms;
    private final TwilioMediaFetcher media;
    private final GeminiService gemini;
    private final ImageNormalizer images;
    private final PhotoStorage storage;
    private final ReportService reports;
    private final TrashPhotoRepository photos;
    private final ClanMembership clans;
    private final Clock clock;

    public WhatsAppBot(WhatsAppSessions sessions, Pseudonyms pseudonyms, TwilioMediaFetcher media,
                       ImageNormalizer images, GeminiService gemini, PhotoStorage storage, ReportService reports,
                       TrashPhotoRepository photos, ClanMembership clans, Clock clock) {
        this.sessions = sessions;
        this.pseudonyms = pseudonyms;
        this.media = media;
        this.gemini = gemini;
        this.images = images;
        this.storage = storage;
        this.reports = reports;
        this.photos = photos;
        this.clans = clans;
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
        if (message.body() != null) {
            Matcher clan = CLAN_COMMAND.matcher(message.body());
            if (clan.matches()) {
                return handleClan(senderKey, clan.group(1));
            }
        }
        return WELCOME;
    }

    private String handleClan(String senderKey, String code) {
        if (!code.isEmpty()) {
            Optional<Faculty> joined = clans.join(senderKey, code);
            if (joined.isPresent()) {
                Faculty f = joined.get();
                return "You're now reporting for " + f.getEmoji() + " " + f.getName()
                        + ". Every report you send earns them points!";
            }
        }
        String current = clans.clanOf(senderKey).map(f -> "Your clan: " + f.getShortCode() + " " + f.getEmoji() + "\n\n").orElse("");
        String options = clans.all().stream()
                .map(f -> f.getShortCode() + " " + f.getEmoji() + " " + f.getName())
                .collect(Collectors.joining("\n"));
        return (code.isEmpty() ? "" : "There's no clan called \"" + code + "\".\n\n")
                + current + "Reply \"clan CODE\" to join one of:\n" + options;
    }

    private String handlePhoto(String senderKey, String publicId, String mediaUrl) {
        Optional<WhatsAppSessions.Location> location = sessions.current(senderKey);
        if (location.isEmpty()) {
            return NEED_LOCATION;
        }

        ImageNormalizer.Normalized photo;
        try {
            photo = images.normalize(media.fetch(mediaUrl).bytes());
        } catch (SafeHttpFetcher.FetchException | ImageNormalizer.UnreadableImageException e) {
            log.warn("Refused WhatsApp media from {}: {}", publicId, e.getMessage());
            return BAD_MEDIA;
        }

        GeminiService.ScoreResult ai = gemini.scorePhoto(photo.bytes(), photo.contentType());
        if (ai.fake()) {
            log.info("Rejected a photo from {} as not genuine: {}", publicId, ai.fakeReason());
            return rejection(ai.fakeReason());
        }

        String photoUrl = storage.store(photo, publicId);
        file(location.get(), ai, photoUrl, publicId, clans.clanOf(senderKey).orElse(null));
        sessions.finish(senderKey);
        return verdict(ai);
    }

    private void file(WhatsAppSessions.Location at, GeminiService.ScoreResult ai, String photoUrl,
                      String publicId, Faculty clan) {
        String tags = ai.tags().isEmpty() ? null : String.join(",", ai.tags());
        reports.submit(new ReportService.Submission(
                at.lat(), at.lng(), ai.score(), "WhatsApp report", photoUrl, tags,
                clan == null ? null : clan.getShortCode(), publicId));

        TrashPhoto feedItem = new TrashPhoto();
        feedItem.setUsername("WhatsApp user");
        feedItem.setPhotoUrl(photoUrl);
        feedItem.setLat(at.lat());
        feedItem.setLng(at.lng());
        feedItem.setCleanlinessScore(ai.score());
        feedItem.setUserRating(CleanlinessScale.toRating(ai.score()));
        feedItem.setTags(tags);
        if (clan != null) {
            feedItem.setFacultyShortCode(clan.getShortCode());
            feedItem.setFacultyEmoji(clan.getEmoji());
            feedItem.setFacultyColor(clan.getColor());
        }
        feedItem.setReportedAt(clock.instant());
        photos.save(feedItem);
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
