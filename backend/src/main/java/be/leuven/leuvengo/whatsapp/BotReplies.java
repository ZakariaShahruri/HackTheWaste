package be.leuven.leuvengo.whatsapp;

import be.leuven.leuvengo.domain.CleanlinessScale;
import be.leuven.leuvengo.domain.Faculty;
import be.leuven.leuvengo.service.GeminiService;
import be.leuven.leuvengo.service.PointsPolicy;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Everything the WhatsApp bot says. */
final class BotReplies {

    private static final String SHARE_PIN = "a 📍 location pin (tap 📎 or +, then Location)";

    static final String MENU = """
            Here's what you can send:

            📍 a location pin, then 📸 a photo: report litter
            *clan*: see or change your clan
            *reset*: drop the pin you sent
            *menu*: this list""";
    static final String NEED_LOCATION = "We don't have a location yet. Please share " + SHARE_PIN
            + " first, then send the photo again.";
    static final String ASK_FOR_PHOTO = "Got it! 📍 Now please send a photo of the litter "
            + "so our AI can assess it.";
    static final String OUTSIDE_LEUVEN = "That location is outside Leuven. Leuven Go only covers the city of "
            + "Leuven, so please share a 📍 pin from there.";
    static final String BAD_MEDIA = "We couldn't open that photo. Please send it again as a normal photo.";
    static final String SERVER_ERROR = "Something went wrong on our side. Please send the photo again in a moment.";
    static final String RESET = "Done, we've dropped your pin. Share a new 📍 one whenever you like.";
    static final String NOTHING_TO_RESET = "There's nothing to drop. Share " + SHARE_PIN + " to start a report.";

    /** How much of someone's own words we repeat back to them. */
    private static final int MAX_ECHO = 30;

    private BotReplies() {}

    static String welcome(Optional<String> firstName, boolean pinWaiting, Optional<Faculty> clan) {
        String steps = pinWaiting
                ? "We still have the 📍 pin you sent, so just send a 📸 photo of the litter."
                : "To report litter:\n1. Share " + SHARE_PIN + "\n2. Send a 📸 photo of it";
        String clanLine = clan
                .map(c -> "You're reporting for " + badge(c) + ".")
                .orElse("Reply *clan* to pick the clan your reports earn points for.");
        return "Hi" + firstName.map(n -> " " + shorten(n)).orElse("") + "! 👋 Welcome to Leuven Go.\n\n"
                + steps + "\n\n" + clanLine + "\nReply *menu* to see everything you can send.";
    }

    static String clanList(Optional<Faculty> current, List<Faculty> all) {
        return current.map(c -> "Your clan: " + badge(c) + "\n\n").orElse("")
                + "Reply with a code or a name to join a clan:\n"
                + all.stream().map(c -> c.getShortCode() + " " + badge(c)).collect(Collectors.joining("\n"));
    }

    static String noSuchClan(String choice, List<Faculty> all) {
        return "There's no clan called \"" + shorten(choice) + "\".\n\n" + clanList(Optional.empty(), all);
    }

    static String joined(ClanMembership.Standing standing, Optional<String> firstName) {
        Faculty clan = standing.clan();
        return "Welcome to " + badge(clan) + firstName.map(n -> ", " + shorten(n)).orElse("") + "!\n\n"
                + clan.getName() + " is #" + standing.rank() + " with " + standing.points() + " points. "
                + "Each report you send earns them " + PointsPolicy.MIN + " to " + PointsPolicy.MAX + " points.\n\n"
                + "Share a 📍 location pin to send one.";
    }

    static String rejected(String reason) {
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

    private static String badge(Faculty clan) {
        return clan.getEmoji() + " " + clan.getName();
    }

    private static String shorten(String text) {
        return text.length() <= MAX_ECHO ? text : text.substring(0, MAX_ECHO) + "…";
    }
}
