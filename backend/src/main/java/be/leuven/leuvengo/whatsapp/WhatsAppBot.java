package be.leuven.leuvengo.whatsapp;

import be.leuven.leuvengo.domain.Faculty;
import be.leuven.leuvengo.domain.Report;
import be.leuven.leuvengo.net.SafeHttpFetcher;
import be.leuven.leuvengo.security.Pseudonyms;
import be.leuven.leuvengo.service.LeuvenArea;
import be.leuven.leuvengo.service.ReportIngestionService;
import be.leuven.leuvengo.service.ReportIngestionService.Filed;
import be.leuven.leuvengo.service.ReportIngestionService.Rejected;
import be.leuven.leuvengo.service.Reporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            return first.isEmpty() ? Optional.empty() : Optional.of(first);
        }
    }

    private static final Pattern CLAN_COMMAND = Pattern.compile("(?is)clan\\b[\\s:]*(.*)");
    /** Also what the website's WhatsApp button sends, so it never counts as picking a clan. */
    private static final Pattern GREETING = Pattern.compile("(?is)(hi|hey|hello|hallo|hoi)\\b.*");

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
            return BotReplies.MENU;
        }
        String senderKey = pseudonyms.senderKey(message.from());
        String publicId = pseudonyms.publicId(message.from());

        if (message.hasLocation()) {
            log.info("WhatsApp location from {}", publicId);
            return handleLocation(senderKey, message.lat(), message.lng());
        }
        if (message.hasMedia()) {
            log.info("WhatsApp photo from {}", publicId);
            return handlePhoto(senderKey, publicId, message.mediaUrl());
        }
        return handleText(senderKey, message.text(), message.firstName());
    }

    private String handleText(String senderKey, String text, Optional<String> firstName) {
        switch (text.toLowerCase(Locale.ROOT)) {
            // Twilio can keep "help" and "cancel" to itself, so the bot only advertises the other two.
            case "menu", "help" -> {
                return BotReplies.MENU;
            }
            case "reset", "cancel" -> {
                return reset(senderKey);
            }
            default -> { }
        }
        Matcher clanCommand = CLAN_COMMAND.matcher(text);
        if (clanCommand.matches()) {
            String choice = clanCommand.group(1);
            return choice.isEmpty() ? listClans(senderKey) : joinClan(senderKey, choice, firstName);
        }
        if (GREETING.matcher(text).matches()) {
            clans.stopChoosing(senderKey);
        } else if (!text.isEmpty() && clans.isChoosing(senderKey)) {
            return joinClan(senderKey, text, firstName);
        }
        return BotReplies.welcome(firstName, sessions.current(senderKey).isPresent(), clans.clanOf(senderKey));
    }

    private String handleLocation(String senderKey, double lat, double lng) {
        if (!LeuvenArea.contains(lat, lng)) {
            return BotReplies.OUTSIDE_LEUVEN;
        }
        clans.stopChoosing(senderKey);
        sessions.start(senderKey, lat, lng);
        return BotReplies.ASK_FOR_PHOTO;
    }

    private String handlePhoto(String senderKey, String publicId, String mediaUrl) {
        Optional<WhatsAppSessions.Location> location = sessions.current(senderKey);
        if (location.isEmpty()) {
            return BotReplies.NEED_LOCATION;
        }

        byte[] image;
        try {
            image = media.fetch(mediaUrl).bytes();
        } catch (SafeHttpFetcher.FetchException e) {
            log.warn("Refused WhatsApp media from {}: {}", publicId, e.getMessage());
            return BotReplies.BAD_MEDIA;
        }

        String clan = clans.clanOf(senderKey).map(Faculty::getShortCode).orElse(null);
        ReportIngestionService.Outcome outcome;
        try {
            outcome = ingestion.ingest(new ReportIngestionService.PhotoReport(
                    location.get().lat(), location.get().lng(), image, Report.Source.WHATSAPP,
                    Reporter.anonymous(publicId, clan)));
        } catch (RuntimeException e) {
            log.error("Could not file WhatsApp report from {}", publicId, e);
            return BotReplies.SERVER_ERROR;
        }

        return switch (outcome) {
            case Filed filed -> {
                sessions.finish(senderKey);
                yield BotReplies.verdict(filed.verdict());
            }
            case Rejected rejected -> switch (rejected.reason()) {
                case UNREADABLE_IMAGE -> BotReplies.BAD_MEDIA;
                case NOT_GENUINE -> BotReplies.rejected(rejected.detail());
                case OUTSIDE_LEUVEN -> BotReplies.OUTSIDE_LEUVEN;
            };
        };
    }

    private String joinClan(String senderKey, String choice, Optional<String> firstName) {
        Optional<Faculty> joined = clans.join(senderKey, choice);
        if (joined.isEmpty()) {
            clans.startChoosing(senderKey);
            return BotReplies.noSuchClan(choice, clans.all());
        }
        return BotReplies.joined(clans.standing(joined.get()), firstName);
    }

    private String listClans(String senderKey) {
        clans.startChoosing(senderKey);
        return BotReplies.clanList(clans.clanOf(senderKey), clans.all());
    }

    private String reset(String senderKey) {
        boolean pinWaiting = sessions.current(senderKey).isPresent();
        clans.stopChoosing(senderKey);
        sessions.finish(senderKey);
        return pinWaiting ? BotReplies.RESET : BotReplies.NOTHING_TO_RESET;
    }
}
