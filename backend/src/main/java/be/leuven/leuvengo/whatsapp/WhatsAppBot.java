package be.leuven.leuvengo.whatsapp;

import be.leuven.leuvengo.domain.CleanlinessScale;
import be.leuven.leuvengo.domain.Faculty;
import be.leuven.leuvengo.domain.Report;
import be.leuven.leuvengo.net.SafeHttpFetcher;
import be.leuven.leuvengo.security.Pseudonyms;
import be.leuven.leuvengo.service.GeminiService;
import be.leuven.leuvengo.service.LeuvenArea;
import be.leuven.leuvengo.service.PointsPolicy;
import be.leuven.leuvengo.service.ReportIngestionService;
import be.leuven.leuvengo.service.ReportIngestionService.Filed;
import be.leuven.leuvengo.service.ReportIngestionService.Rejected;
import be.leuven.leuvengo.service.Reporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The WhatsApp conversation, independent of Twilio's HTTP details:
 * <ol>
 *   <li>the user shares a location pin, and we remember it for 30 minutes;</li>
 *   <li>the user sends a photo, which goes through {@link ReportIngestionService}, and we reply with the verdict.</li>
 * </ol>
 * Sending {@code clan} lists the faculties; the sender picks one by code or name, and it then earns points for
 * their reports.
 * Phone numbers are only used to derive pseudonyms; they are never stored or logged.
 */
@Service
public class WhatsAppBot {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppBot.class);

    /** One incoming message, as Twilio describes it. Every field may be null. */
    public record Inbound(String from, String profileName, String body, Double lat, Double lng, String mediaUrl) {

        boolean hasLocation() {
            return lat != null && lng != null;
        }

        boolean hasMedia() {
            return mediaUrl != null && !mediaUrl.isBlank();
        }

        String text() {
            return body == null ? "" : body.strip();
        }

        /** The first word of their WhatsApp name, or empty. Only for replies: it is never stored or logged. */
        Optional<String> firstName() {
            if (profileName == null) return Optional.empty();
            String first = profileName.replaceAll("[\\p{Cntrl}*_~`]", " ").strip().split("\\s+")[0];
            return first.isEmpty() ? Optional.empty() : Optional.of(shorten(first));
        }
    }

    static final String WELCOME = "Hi! Send a 📍 location pin to start a cleanliness report.\n\n"
            + "Reply \"clan\" to pick the faculty your reports earn points for.";
    static final String NEED_LOCATION = "We don't have a location yet. Please share a 📍 location pin first, "
            + "then send the photo again.";
    static final String ASK_FOR_PHOTO = "Got it! 📍 Now please send a photo of the litter "
            + "so our AI can assess it.";
    static final String OUTSIDE_LEUVEN = "That location is outside Leuven. Leuven Go only covers the city of "
            + "Leuven, so please share a 📍 pin from there.";
    static final String BAD_MEDIA = "We couldn't open that photo. Please send it again as a normal photo.";
    static final String SERVER_ERROR = "Something went wrong on our side. Please send the photo again in a moment.";
    /** How much of someone's own words we repeat back to them. */
    private static final int MAX_ECHO = 30;
    private static final Pattern CLAN_COMMAND = Pattern.compile("(?is)clan\\b[\\s:]*(.*)");

    private final WhatsAppSessions sessions;
    private final Pseudonyms pseudonyms;
    private final TwilioMediaFetcher media;
    private final ReportIngestionService ingestion;
    private final ClanMembership clans;

    public WhatsAppBot(WhatsAppSessions sessions, Pseudonyms pseudonyms, TwilioMediaFetcher media,
                       ReportIngestionService ingestion, ClanMembership clans) {
        this.sessions = sessions;
        this.pseudonyms = pseudonyms;
        this.media = media;
        this.ingestion = ingestion;
        this.clans = clans;
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
            if (!LeuvenArea.contains(message.lat(), message.lng())) {
                return OUTSIDE_LEUVEN;
            }
            clans.stopChoosing(senderKey);
            sessions.start(senderKey, message.lat(), message.lng());
            return ASK_FOR_PHOTO;
        }
        if (message.hasMedia()) {
            log.info("WhatsApp photo from {}", publicId);
            return handlePhoto(senderKey, publicId, message.mediaUrl());
        }

        String text = message.text();
        Matcher clanCommand = CLAN_COMMAND.matcher(text);
        if (clanCommand.matches()) {
            String choice = clanCommand.group(1);
            return choice.isEmpty() ? listClans(senderKey) : joinClan(senderKey, choice, message.firstName());
        }
        if (!text.isEmpty() && clans.isChoosing(senderKey)) {
            return joinClan(senderKey, text, message.firstName());
        }
        return WELCOME;
    }

    private String handlePhoto(String senderKey, String publicId, String mediaUrl) {
        Optional<WhatsAppSessions.Location> location = sessions.current(senderKey);
        if (location.isEmpty()) {
            return NEED_LOCATION;
        }

        byte[] image;
        try {
            image = media.fetch(mediaUrl).bytes();
        } catch (SafeHttpFetcher.FetchException e) {
            log.warn("Refused WhatsApp media from {}: {}", publicId, e.getMessage());
            return BAD_MEDIA;
        }

        String clan = clans.clanOf(senderKey).map(Faculty::getShortCode).orElse(null);
        ReportIngestionService.Outcome outcome;
        try {
            outcome = ingestion.ingest(new ReportIngestionService.PhotoReport(
                    location.get().lat(), location.get().lng(), image, Report.Source.WHATSAPP,
                    Reporter.anonymous(publicId, clan)));
        } catch (RuntimeException e) {
            log.error("Could not file WhatsApp report from {}", publicId, e);
            return SERVER_ERROR;
        }

        return switch (outcome) {
            case Filed filed -> {
                sessions.finish(senderKey);
                yield verdict(filed.verdict());
            }
            case Rejected rejected -> switch (rejected.reason()) {
                case UNREADABLE_IMAGE -> BAD_MEDIA;
                case NOT_GENUINE -> rejection(rejected.detail());
                case OUTSIDE_LEUVEN -> OUTSIDE_LEUVEN;
            };
        };
    }

    private String joinClan(String senderKey, String choice, Optional<String> firstName) {
        return clans.join(senderKey, choice)
                .map(clan -> joined(clans.standing(clan), firstName))
                .orElseGet(() -> "There's no clan called \"" + shorten(choice) + "\".\n\n" + listClans(senderKey));
    }

    private String listClans(String senderKey) {
        clans.startChoosing(senderKey);
        String current = clans.clanOf(senderKey)
                .map(f -> "Your clan: " + badge(f) + "\n\n")
                .orElse("");
        String options = clans.all().stream()
                .map(f -> f.getShortCode() + " " + badge(f))
                .collect(Collectors.joining("\n"));
        return current + "Reply with a code or a name to join a clan:\n" + options;
    }

    private static String joined(ClanMembership.Standing standing, Optional<String> firstName) {
        Faculty clan = standing.clan();
        return "Welcome to " + badge(clan) + firstName.map(n -> ", " + n).orElse("") + "!\n\n"
                + clan.getName() + " is #" + standing.rank() + " with " + standing.points() + " points. "
                + "Each report you send earns them " + PointsPolicy.MIN + " to " + PointsPolicy.MAX + " points.\n\n"
                + "Share a 📍 location pin to send one.";
    }

    private static String badge(Faculty clan) {
        return clan.getEmoji() + " " + clan.getName();
    }

    private static String shorten(String text) {
        return text.length() <= MAX_ECHO ? text : text.substring(0, MAX_ECHO) + "…";
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
