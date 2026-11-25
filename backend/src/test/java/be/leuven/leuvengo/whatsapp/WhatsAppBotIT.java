package be.leuven.leuvengo.whatsapp;

import be.leuven.leuvengo.domain.Report;
import be.leuven.leuvengo.net.SafeHttpFetcher;
import be.leuven.leuvengo.repository.FacultyRepository;
import be.leuven.leuvengo.repository.PendingReportRepository;
import be.leuven.leuvengo.repository.ReportRepository;
import be.leuven.leuvengo.repository.WhatsAppSenderRepository;
import be.leuven.leuvengo.support.TestImages;
import be.leuven.leuvengo.security.Pseudonyms;
import be.leuven.leuvengo.service.GeminiService;
import be.leuven.leuvengo.service.PointsPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "leuvengo.site-url=https://leuvengo.example")
@Transactional
class WhatsAppBotIT {

    private static final String ANA = "whatsapp:+32470000001";
    private static final String BOB = "whatsapp:+32470000002";
    private static final double LAT = 50.8525, LNG = 4.6720;

    @Autowired WhatsAppBot bot;
    @Autowired WhatsAppSessions sessions;
    @Autowired PendingReportRepository pending;
    @Autowired ReportRepository reports;
    @Autowired Pseudonyms pseudonyms;
    @Autowired FacultyRepository faculties;
    @Autowired WhatsAppSenderRepository senders;
    @MockitoBean GeminiService gemini;
    @MockitoBean TwilioMediaFetcher media;

    private long reportsBefore;

    @BeforeEach
    void setUp() throws Exception {
        reportsBefore = reports.count();
        when(media.fetch(anyString())).thenReturn(new SafeHttpFetcher.Download(TestImages.jpeg(640, 480), "image/jpeg"));
        when(gemini.scorePhoto(any(), anyString())).thenReturn(
                GeminiService.ScoreResult.genuine(10, "Overflowing bin", List.of("plastic")));
    }

    /** Regression: WhatsApp used to store a filthy photo as a "clean" report and vice versa. */
    @Test
    void aFilthyPhotoBecomesADirtyReport() {
        assertThat(location(ANA)).isEqualTo(BotReplies.ASK_FOR_PHOTO);
        String reply = photo(ANA);

        assertThat(reply).contains("10/100", "Overflowing bin", "#plastic");
        Report latest = latestReport();
        assertThat(latest.getCleanlinessScore()).isEqualTo(10);
        assertThat(latest.getHotspot().getMeanCleanliness()).isEqualTo(10.0);
        assertThat(pending.count()).isZero();
    }

    @Test
    void theFeedGetsTheStoredPhoto() {
        location(ANA);
        photo(ANA);

        Report latest = latestReport();
        assertThat(latest.getPhotoUrl()).startsWith("/api/photos/");
        assertThat(latest.getSource()).isEqualTo(Report.Source.WHATSAPP);
        assertThat(latest.getAiDescription()).isEqualTo("Overflowing bin");
    }

    @Test
    void aFileThatIsNotReallyAnImageIsRefused() throws Exception {
        location(ANA);
        when(media.fetch(anyString())).thenReturn(new SafeHttpFetcher.Download("<svg/>".getBytes(), "image/jpeg"));

        assertThat(photo(ANA)).isEqualTo(BotReplies.BAD_MEDIA);
        assertThat(reports.count()).isEqualTo(reportsBefore);
    }

    @Test
    void phoneNumbersAreNeverStored() {
        location(ANA);
        assertThat(pending.findAll()).allSatisfy(p -> assertThat(p.getSenderKey()).hasSize(64).doesNotContain("3247"));
        photo(ANA);

        assertThat(latestReport().getReporterPseudoId()).matches("wa-[0-9a-f]{12}");
    }

    @Test
    void aPhotoNeverUsesSomeoneElsesLocation() {
        location(ANA);

        assertThat(photo(BOB)).isEqualTo(BotReplies.ASK_FOR_PIN);
        assertThat(reports.count()).isEqualTo(reportsBefore);
        assertThat(photo(ANA)).contains("10/100");
    }

    @Test
    void aStaleLocationHasToBeSharedAgain() {
        location(ANA);
        pending.findAll().getFirst().holdPin(LAT, LNG, Instant.now().minus(WhatsAppSessions.TTL).minusSeconds(1));

        assertThat(photo(ANA)).isEqualTo(BotReplies.ASK_FOR_PIN);
    }

    @Test
    void expiredLocationsArePurged() {
        location(ANA);
        location(BOB);
        pending.findAll().getFirst().holdPin(LAT, LNG, Instant.now().minus(WhatsAppSessions.TTL).minusSeconds(1));

        sessions.purgeExpired();

        assertThat(pending.count()).isEqualTo(1);
    }

    @Test
    void aPinOutsideLeuvenIsRefusedStraightAway() {
        assertThat(bot.handle(new WhatsAppBot.Inbound(ANA, null, null, 51.2194, 4.4025, null, null)))
                .isEqualTo(BotReplies.OUTSIDE_LEUVEN);
        assertThat(pending.count()).isZero();
    }

    @Test
    void aNewPinReplacesTheOldOne() {
        location(ANA);
        bot.handle(new WhatsAppBot.Inbound(ANA, null, null, 50.88, 4.70, null, null));

        assertThat(pending.count()).isEqualTo(1);
        assertThat(sessions.current(pseudonyms.senderKey(ANA))).contains(new WhatsAppSessions.Pin(50.88, 4.70));
    }

    @Test
    void anUnreadablePhotoKeepsTheLocationForARetry() throws Exception {
        location(ANA);
        when(media.fetch(anyString())).thenThrow(new SafeHttpFetcher.FetchException("Content type not allowed"));

        assertThat(photo(ANA)).isEqualTo(BotReplies.BAD_MEDIA);
        assertThat(pending.count()).isEqualTo(1);
        assertThat(reports.count()).isEqualTo(reportsBefore);
    }

    @Test
    void aFakePhotoIsRejectedAndNotFiled() {
        location(ANA);
        when(gemini.scorePhoto(any(), anyString())).thenReturn(GeminiService.ScoreResult.rejected("Photo of a screen"));

        assertThat(photo(ANA)).contains("Photo rejected", "Photo of a screen");
        assertThat(reports.count()).isEqualTo(reportsBefore);
        assertThat(pending.count()).isEqualTo(1);
    }

    @Test
    void whenTheAiIsOfflineTheReplySaysSo() {
        location(ANA);
        when(gemini.scorePhoto(any(), anyString())).thenReturn(GeminiService.ScoreResult.unavailable());

        assertThat(photo(ANA)).contains("offline").doesNotContain("/100");
        assertThat(latestReport().getCleanlinessScore()).isEqualTo(50);
    }

    @Test
    void aPhotoSentFirstIsFiledWhenThePinArrives() {
        assertThat(photo(ANA)).isEqualTo(BotReplies.ASK_FOR_PIN);
        assertThat(reports.count()).isEqualTo(reportsBefore);

        assertThat(location(ANA)).contains("10/100", "Overflowing bin");
        assertThat(reports.count()).isEqualTo(reportsBefore + 1);
        assertThat(pending.count()).isZero();
    }

    @Test
    void aPhotoSentFirstStaysWithItsSender() {
        photo(ANA);

        assertThat(location(BOB)).isEqualTo(BotReplies.ASK_FOR_PHOTO);
        assertThat(reports.count()).isEqualTo(reportsBefore);
    }

    @Test
    void aPinOutsideLeuvenKeepsTheWaitingPhoto() {
        photo(ANA);

        assertThat(bot.handle(new WhatsAppBot.Inbound(ANA, null, null, 51.2194, 4.4025, null, null)))
                .isEqualTo(BotReplies.OUTSIDE_LEUVEN);
        assertThat(location(ANA)).contains("10/100");
    }

    @Test
    void whenTheWaitingPhotoIsFakeOnlyANewPhotoIsNeeded() {
        photo(ANA);
        when(gemini.scorePhoto(any(), anyString())).thenReturn(GeminiService.ScoreResult.rejected("Photo of a screen"));
        assertThat(location(ANA)).contains("Photo rejected");

        when(gemini.scorePhoto(any(), anyString())).thenReturn(
                GeminiService.ScoreResult.genuine(10, "Overflowing bin", List.of("plastic")));
        assertThat(photo(ANA)).contains("10/100");
    }

    @Test
    void aStalePhotoHasToBeSentAgain() {
        photo(ANA);
        pending.findAll().getFirst().holdPhoto("https://api.twilio.com/media/1",
                Instant.now().minus(WhatsAppSessions.TTL).minusSeconds(1));

        assertThat(location(ANA)).isEqualTo(BotReplies.ASK_FOR_PHOTO);
    }

    @Test
    void aVoiceNoteIsNotKeptAsThePhoto() {
        assertThat(bot.handle(new WhatsAppBot.Inbound(
                ANA, null, null, null, null, "https://api.twilio.com/media/1", "audio/ogg")))
                .isEqualTo(BotReplies.BAD_MEDIA);
        assertThat(pending.count()).isZero();
    }

    @Test
    void theWelcomeSaysThePhotoIsStillWaiting() {
        photo(ANA);
        assertThat(text(ANA, "hello")).contains("We still have the 📸 photo you sent");
        assertThat(text(ANA, "reset")).isEqualTo(BotReplies.RESET);
        assertThat(location(ANA)).isEqualTo(BotReplies.ASK_FOR_PHOTO);
    }

    @Test
    void joiningAClanCreditsItForLaterReports() {
        int before = faculties.findByShortCode("EKO").orElseThrow().getPoints();

        assertThat(text(ANA, "  Clan eko ")).startsWith("Welcome to 📊 Ekonomika, Ana!");
        location(ANA);
        photo(ANA);

        Report latest = latestReport();
        assertThat(latest.getCreditedFaculty().getShortCode()).isEqualTo("EKO");
        assertThat(faculties.findByShortCode("EKO").orElseThrow().getPoints())
                .isEqualTo(before + PointsPolicy.pointsFor(10));
    }

    @Test
    void theReplyShowsWhatTheReportEarned() {
        faculties.findAll().forEach(f -> f.setPoints(0));
        faculties.findByShortCode("EKO").orElseThrow().setPoints(500);
        faculties.findByShortCode("UCL").orElseThrow().setPoints(400);
        text(ANA, "clan UCL");
        location(ANA);

        assertThat(photo(ANA))
                .contains("+55 points for 🎓 UCLL (now 455, #2)")
                .endsWith("https://leuvengo.example/feed");
    }

    @Test
    void reportsWithoutAClanCreditNobody() {
        location(BOB);
        assertThat(photo(BOB)).contains("Reply *clan* to pick a clan").doesNotContain(" points for ");
        assertThat(latestReport().getCreditedFaculty()).isNull();
    }

    @Test
    void anUnknownClanListsTheRealOnes() {
        assertThat(text(ANA, "clan NOPE"))
                .startsWith("There's no clan called \"NOPE\"")
                .contains("EKO", "Reply with a code or a name");
        assertThat(text(ANA, "clan")).doesNotContain("no clan called").contains("EKO");
    }

    @Test
    void theClanCommandShowsYourCurrentClan() {
        text(ANA, "clan EKO");
        assertThat(text(ANA, "clan")).contains("Your clan: 📊 Ekonomika");
    }

    /** Regression: a bare code or name used to get the generic welcome. */
    @Test
    void afterTheListACodeOrANameIsEnough() {
        for (String pick : List.of("UCL", "UCLL", " ucll ")) {
            text(ANA, "clan");
            assertThat(text(ANA, pick)).startsWith("Welcome to 🎓 UCLL, Ana!");
        }
    }

    @Test
    void aWrongPickGetsTheListAgainAndAnotherTry() {
        text(ANA, "clan");

        assertThat(text(ANA, "UGent")).startsWith("There's no clan called \"UGent\"").contains("PED 🚴 Pedal");
        assertThat(text(ANA, "PED")).startsWith("Welcome to");
    }

    @Test
    void aCodeOnItsOwnMeansNothingWithoutTheList() {
        assertThat(text(ANA, "PED")).startsWith("Hi Ana! 👋 Welcome to Leuven Go.");

        text(BOB, "clan");
        assertThat(text(ANA, "PED")).startsWith("Hi Ana! 👋 Welcome to Leuven Go.");
    }

    @Test
    void theListIsForgottenOnceTheyJoinSendAPinOrWaitTooLong() {
        text(ANA, "clan EKO");
        assertThat(text(ANA, "PED")).startsWith("Hi Ana! 👋 Welcome to Leuven Go.");

        text(ANA, "clan");
        location(ANA);
        assertThat(text(ANA, "PED")).startsWith("Hi Ana! 👋 Welcome to Leuven Go.");

        text(ANA, "clan");
        senders.findBySenderKey(pseudonyms.senderKey(ANA)).orElseThrow()
                .startChoosing(Instant.now().minus(ClanMembership.CHOICE_TTL).minusSeconds(1));
        assertThat(text(ANA, "PED")).startsWith("Hi Ana! 👋 Welcome to Leuven Go.");
    }

    @Test
    void joiningShowsTheClansPlaceOnTheLeaderboard() {
        faculties.findAll().forEach(f -> f.setPoints(0));
        faculties.findByShortCode("EKO").orElseThrow().setPoints(500);
        faculties.findByShortCode("PED").orElseThrow().setPoints(412);

        assertThat(text(ANA, "clan PED")).contains("Pedal is #2 with 412 points", "earns them 10 to 60 points");
    }

    @Test
    void namesAreCleanedUpAndOptional() {
        assertThat(named("*Zoë*\n_van_ Dam")).startsWith("Welcome to 🚴 Pedal, Zoë!");
        assertThat(named("x".repeat(80))).startsWith("Welcome to 🚴 Pedal, " + "x".repeat(30) + "…!");
        assertThat(named("  ")).startsWith("Welcome to 🚴 Pedal!");
        assertThat(named(null)).startsWith("Welcome to 🚴 Pedal!");
    }

    @Test
    void otherTextGetsTheWelcome() {
        assertThat(text(ANA, "hello")).startsWith("Hi Ana! 👋 Welcome to Leuven Go.");
        assertThat(text(ANA, "clans are cool")).startsWith("Hi Ana! 👋 Welcome to Leuven Go.");
    }

    @Test
    void theWelcomeDependsOnWhereTheSenderIs() {
        assertThat(text(ANA, "hello")).contains("To report litter", "Reply *clan* to pick");

        text(ANA, "clan UCL");
        location(ANA);
        assertThat(text(ANA, "hello"))
                .contains("We still have the 📍 pin you sent", "You're reporting for 🎓 UCLL")
                .doesNotContain("To report litter");
    }

    /** The text the website's WhatsApp button starts with. */
    @Test
    void aGreetingIsNeverTakenForAClan() {
        text(ANA, "clan");

        assertThat(text(ANA, "Hi! I'd like to report litter in Leuven.")).startsWith("Hi Ana!");
        assertThat(text(ANA, "UCL")).startsWith("Hi Ana!");
    }

    @Test
    void theMenuListsEverything() {
        for (String command : List.of("menu", "Menu ", "help")) {
            assertThat(text(ANA, command)).isEqualTo(BotReplies.MENU);
        }
    }

    @Test
    void resetDropsThePinAndTheClanList() {
        for (String command : List.of("reset", "Cancel")) {
            location(ANA);
            text(ANA, "clan");

            assertThat(text(ANA, command)).isEqualTo(BotReplies.RESET);
            assertThat(pending.count()).isZero();
            assertThat(text(ANA, "UCL")).startsWith("Hi Ana!");
        }
        assertThat(text(ANA, "reset")).isEqualTo(BotReplies.NOTHING_TO_RESET);
    }

    @Test
    void resetLeavesOtherSendersAlone() {
        location(BOB);
        text(ANA, "reset");
        assertThat(photo(BOB)).contains("10/100");
    }

    private String named(String profileName) {
        return bot.handle(new WhatsAppBot.Inbound(ANA, profileName, "clan PED", null, null, null, null));
    }

    private String text(String from, String body) {
        return bot.handle(new WhatsAppBot.Inbound(from, "Ana Peeters", body, null, null, null, null));
    }

    private String location(String from) {
        return bot.handle(new WhatsAppBot.Inbound(from, null, null, LAT, LNG, null, null));
    }

    private String photo(String from) {
        return bot.handle(new WhatsAppBot.Inbound(from, null, null, null, null, "https://api.twilio.com/media/1", null));
    }

    private Report latestReport() {
        return reports.findAll().stream().max(Comparator.comparing(Report::getId)).orElseThrow();
    }
}
