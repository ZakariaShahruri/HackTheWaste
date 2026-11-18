package be.leuven.leuvengo.whatsapp;

import be.leuven.leuvengo.domain.Report;
import be.leuven.leuvengo.net.SafeHttpFetcher;
import be.leuven.leuvengo.repository.FacultyRepository;
import be.leuven.leuvengo.repository.PendingReportRepository;
import be.leuven.leuvengo.repository.ReportRepository;
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

@SpringBootTest
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
    @MockitoBean GeminiService gemini;
    @MockitoBean TwilioMediaFetcher media;

    private long reportsBefore;

    @BeforeEach
    void setUp() throws Exception {
        reportsBefore = reports.count();
        when(media.fetch(anyString())).thenReturn(new SafeHttpFetcher.Download(new byte[]{1}, "image/jpeg"));
        when(gemini.scorePhoto(any(), anyString())).thenReturn(
                GeminiService.ScoreResult.genuine(10, "Overflowing bin", List.of("plastic")));
    }

    /** Regression: WhatsApp used to store a filthy photo as a "clean" report and vice versa. */
    @Test
    void aFilthyPhotoBecomesADirtyReport() {
        assertThat(location(ANA)).isEqualTo(WhatsAppBot.ASK_FOR_PHOTO);
        String reply = photo(ANA);

        assertThat(reply).contains("10/100", "Overflowing bin", "#plastic");
        Report latest = latestReport();
        assertThat(latest.getCleanlinessScore()).isEqualTo(10);
        assertThat(latest.getHotspot().getMeanCleanliness()).isEqualTo(10.0);
        assertThat(pending.count()).isZero();
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

        assertThat(photo(BOB)).isEqualTo(WhatsAppBot.NEED_LOCATION);
        assertThat(reports.count()).isEqualTo(reportsBefore);
        assertThat(photo(ANA)).contains("10/100");
    }

    @Test
    void aStaleLocationHasToBeSharedAgain() {
        location(ANA);
        pending.findAll().getFirst().moveTo(LAT, LNG, Instant.now().minus(WhatsAppSessions.TTL).minusSeconds(1));

        assertThat(photo(ANA)).isEqualTo(WhatsAppBot.NEED_LOCATION);
    }

    @Test
    void expiredLocationsArePurged() {
        location(ANA);
        location(BOB);
        pending.findAll().getFirst().moveTo(LAT, LNG, Instant.now().minus(WhatsAppSessions.TTL).minusSeconds(1));

        sessions.purgeExpired();

        assertThat(pending.count()).isEqualTo(1);
    }

    @Test
    void aNewPinReplacesTheOldOne() {
        location(ANA);
        bot.handle(new WhatsAppBot.Inbound(ANA, null, 50.88, 4.70, null));

        assertThat(pending.count()).isEqualTo(1);
        assertThat(sessions.current(pseudonyms.senderKey(ANA)).orElseThrow().lat()).isEqualTo(50.88);
    }

    @Test
    void anUnreadablePhotoKeepsTheLocationForARetry() throws Exception {
        location(ANA);
        when(media.fetch(anyString())).thenThrow(new SafeHttpFetcher.FetchException("Content type not allowed"));

        assertThat(photo(ANA)).isEqualTo(WhatsAppBot.BAD_MEDIA);
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
    void joiningAClanCreditsItForLaterReports() {
        int before = faculties.findByShortCode("EKO").orElseThrow().getPoints();

        assertThat(text(ANA, "  Clan eko ")).contains("now reporting for");
        location(ANA);
        photo(ANA);

        Report latest = latestReport();
        assertThat(latest.getCrediteFaculty().getShortCode()).isEqualTo("EKO");
        assertThat(faculties.findByShortCode("EKO").orElseThrow().getPoints())
                .isEqualTo(before + PointsPolicy.pointsFor(10));
    }

    @Test
    void reportsWithoutAClanCreditNobody() {
        location(BOB);
        photo(BOB);
        assertThat(latestReport().getCrediteFaculty()).isNull();
    }

    @Test
    void anUnknownClanListsTheRealOnes() {
        assertThat(text(ANA, "clan NOPE"))
                .startsWith("There's no clan called \"NOPE\"")
                .contains("EKO", "Reply \"clan CODE\"");
        assertThat(text(ANA, "clan")).doesNotContain("no clan called").contains("EKO");
    }

    @Test
    void theClanCommandShowsYourCurrentClan() {
        text(ANA, "clan EKO");
        assertThat(text(ANA, "clan")).contains("Your clan: EKO");
    }

    @Test
    void otherTextGetsTheWelcome() {
        assertThat(text(ANA, "hello")).isEqualTo(WhatsAppBot.WELCOME);
        assertThat(text(ANA, "clans are cool")).isEqualTo(WhatsAppBot.WELCOME);
    }

    private String text(String from, String body) {
        return bot.handle(new WhatsAppBot.Inbound(from, body, null, null, null));
    }

    private String location(String from) {
        return bot.handle(new WhatsAppBot.Inbound(from, null, LAT, LNG, null));
    }

    private String photo(String from) {
        return bot.handle(new WhatsAppBot.Inbound(from, null, null, null, "https://api.twilio.com/media/1"));
    }

    private Report latestReport() {
        return reports.findAll().stream().max(Comparator.comparing(Report::getId)).orElseThrow();
    }
}
