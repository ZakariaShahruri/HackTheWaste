package be.leuven.leuvengo.web;

import be.leuven.leuvengo.domain.AppUser;
import be.leuven.leuvengo.domain.CleanlinessScale;
import be.leuven.leuvengo.domain.Report;
import be.leuven.leuvengo.repository.FacultyRepository;
import be.leuven.leuvengo.security.CurrentUser;
import be.leuven.leuvengo.security.RateLimited;
import be.leuven.leuvengo.service.GeminiService;
import be.leuven.leuvengo.service.ReportIngestionService;
import be.leuven.leuvengo.service.ReportIngestionService.Filed;
import be.leuven.leuvengo.service.ReportIngestionService.Rejected;
import be.leuven.leuvengo.service.Reporter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * "Try it in your browser": the same photo pipeline as WhatsApp, for visitors
 * without a WhatsApp setup. Logged-in users have their own clan credited.
 */
@RestController
@RequestMapping("/api/demo-reports")
public class DemoReportController {

    static final String OFFLINE_LABEL = "AI offline, demo score";

    private final ReportIngestionService ingestion;
    private final FacultyRepository faculties;

    public DemoReportController(ReportIngestionService ingestion, FacultyRepository faculties) {
        this.ingestion = ingestion;
        this.faculties = faculties;
    }

    /** What the browser shows after an upload. */
    public record DemoReportResult(long reportId, long hotspotId, String photoUrl,
                                   boolean aiAvailable, int score, String label,
                                   String description, List<String> tags) {}

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @RateLimited(value = "demo-report", requests = 5, perSeconds = 3600)
    public DemoReportResult submit(@RequestParam("photo") MultipartFile photo,
                                   @RequestParam double lat,
                                   @RequestParam double lng,
                                   @RequestParam(required = false) String clan,
                                   @CurrentUser(required = false) AppUser user) throws IOException {
        if (photo.isEmpty()) {
            throw badRequest("Please attach a photo");
        }
        var outcome = ingestion.ingest(new ReportIngestionService.PhotoReport(
                lat, lng, photo.getBytes(), Report.Source.WEB, reporter(user, clan)));

        return switch (outcome) {
            case Filed filed -> result(filed);
            case Rejected rejected -> throw switch (rejected.reason()) {
                case OUTSIDE_LEUVEN -> badRequest("Pick a spot inside Leuven");
                case UNREADABLE_IMAGE -> badRequest("That file isn't a photo we can read");
                case NOT_GENUINE -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                        "Our AI doesn't think this is a genuine on-site photo"
                                + (rejected.detail() == null ? "" : ": " + rejected.detail()));
            };
        };
    }

    /** An explicit clan wins; otherwise a logged-in user's own clan. */
    private String clanFor(String requested, AppUser user) {
        if (requested != null && !requested.isBlank()) {
            String code = requested.trim().toUpperCase(Locale.ROOT);
            if (faculties.findByShortCode(code).isEmpty()) {
                throw badRequest("Unknown clan " + code);
            }
            return code;
        }
        return user == null ? null : user.getFacultyShortCode();
    }

    /** Logged-in reporters earn points themselves too; anonymous ones get a throwaway id. */
    private Reporter reporter(AppUser user, String clan) {
        if (user != null) {
            return new Reporter("user-" + user.getId(), clanFor(clan, user), user.getId());
        }
        byte[] random = new byte[6];
        ThreadLocalRandom.current().nextBytes(random);
        return Reporter.anonymous("web-" + HexFormat.of().formatHex(random), clanFor(clan, null));
    }

    private static DemoReportResult result(Filed filed) {
        GeminiService.ScoreResult verdict = filed.verdict();
        String label = verdict.aiAvailable() ? CleanlinessScale.label(verdict.score()) : OFFLINE_LABEL;
        return new DemoReportResult(filed.reportId(), filed.hotspotId(), filed.photoUrl(),
                verdict.aiAvailable(), verdict.score(), label, verdict.description(), verdict.tags());
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
