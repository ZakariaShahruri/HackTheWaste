package be.leuven.leuvengo.web;

import be.leuven.leuvengo.domain.Report;
import be.leuven.leuvengo.repository.ReportRepository;
import be.leuven.leuvengo.security.AuthService;
import be.leuven.leuvengo.service.GeminiService;
import be.leuven.leuvengo.support.TestImages;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DemoReportIT {

    /** Each test gets its own client IP so rate-limit budgets don't leak between tests. */
    private static final AtomicInteger IP = new AtomicInteger(1);

    @Autowired MockMvc mvc;
    @Autowired ReportRepository reports;
    @Autowired AuthService auth;
    @MockitoBean GeminiService gemini;

    private final String ip = "10.1.0." + IP.getAndIncrement();

    @BeforeEach
    void aiSaysItIsFilthy() {
        when(gemini.scorePhoto(any(), anyString())).thenReturn(
                GeminiService.ScoreResult.genuine(15, "Litter all over the pavement", List.of("plastic")));
    }

    @Test
    void anUploadIsScoredFiledAndShownInTheFeed() throws Exception {
        String body = upload(photo(), "50.8798", "4.7005", null, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.aiAvailable").value(true))
                .andExpect(jsonPath("$.score").value(15))
                .andExpect(jsonPath("$.label").value("Critical"))
                .andExpect(jsonPath("$.tags[0]").value("plastic"))
                .andReturn().getResponse().getContentAsString();

        Report report = reports.findById(((Number) JsonPath.read(body, "$.reportId")).longValue()).orElseThrow();
        assertThat(report.getSource()).isEqualTo(Report.Source.WEB);
        assertThat(report.getReporterPseudoId()).matches("web-[0-9a-f]{12}");

        String photoUrl = JsonPath.read(body, "$.photoUrl");
        mvc.perform(get(photoUrl)).andExpect(status().isOk());
        mvc.perform(get("/api/feed").param("size", "1"))
                .andExpect(jsonPath("$.content[0].id").value(report.getId()))
                .andExpect(jsonPath("$.content[0].username").value("Web reporter"));
    }

    @Test
    void theOfflineAiIsLabelledAsSuch() throws Exception {
        when(gemini.scorePhoto(any(), anyString())).thenReturn(GeminiService.ScoreResult.unavailable());

        upload(photo(), "50.8798", "4.7005", null, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.aiAvailable").value(false))
                .andExpect(jsonPath("$.label").value(DemoReportController.OFFLINE_LABEL));
    }

    @Test
    void aLoggedInUserCreditsTheirOwnClan() throws Exception {
        String token = auth.register("demo-" + ip + "@example.be", "secret-pass", "EKO", true).token();

        String body = upload(photo(), "50.8798", "4.7005", null, token)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Report report = reports.findById(((Number) JsonPath.read(body, "$.reportId")).longValue()).orElseThrow();
        assertThat(report.getReporterPseudoId()).startsWith("user-");
        assertThat(clanOf(report)).isEqualTo("EKO");
    }

    @Test
    void anExplicitClanIsCheckedAndUsed() throws Exception {
        upload(photo(), "50.8798", "4.7005", "nope", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown clan NOPE"));

        String body = upload(photo(), "50.8798", "4.7005", "apo", null)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Report report = reports.findById(((Number) JsonPath.read(body, "$.reportId")).longValue()).orElseThrow();
        assertThat(clanOf(report)).isEqualTo("APO");
    }

    @Test
    void badInputIsA400WithAClearMessage() throws Exception {
        upload(photo(), "51.2194", "4.4025", null, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Pick a spot inside Leuven"));
        upload(new MockMultipartFile("photo", "x.jpg", "image/jpeg", "<html>".getBytes()), "50.8798", "4.7005", null, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("That file isn't a photo we can read"));
        upload(new MockMultipartFile("photo", "x.jpg", "image/jpeg", new byte[0]), "50.8798", "4.7005", null, null)
                .andExpect(status().isBadRequest());
        mvc.perform(withIp(multipart("/api/demo-reports").file(photo()).param("lat", "50.88")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aFakePhotoIsRefusedWithTheReason() throws Exception {
        when(gemini.scorePhoto(any(), anyString())).thenReturn(GeminiService.ScoreResult.rejected("Photo of a monitor"));

        upload(photo(), "50.8798", "4.7005", null, null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value(
                        "Our AI doesn't think this is a genuine on-site photo: Photo of a monitor"));
    }

    @Test
    void onlyFiveUploadsPerHourPerVisitor() throws Exception {
        for (int i = 0; i < 5; i++) {
            upload(photo(), "50.8798", "4.7005", null, null).andExpect(status().isCreated());
        }
        upload(photo(), "50.8798", "4.7005", null, null)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    private String clanOf(Report report) {
        return reports.findByPhotoUrlIsNotNullOrderByReportedAtDesc(org.springframework.data.domain.PageRequest.of(0, 50))
                .stream().filter(r -> r.getId().equals(report.getId())).findFirst().orElseThrow()
                .getCreditedFaculty().getShortCode();
    }

    private ResultActions upload(MockMultipartFile file, String lat, String lng, String clan, String token) throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart("/api/demo-reports").file(file);
        request.param("lat", lat).param("lng", lng);
        if (clan != null) request.param("clan", clan);
        if (token != null) request.header("Authorization", "Bearer " + token);
        return mvc.perform(withIp(request));
    }

    private MockMultipartHttpServletRequestBuilder withIp(MockMultipartHttpServletRequestBuilder builder) {
        builder.with(req -> {
            req.setRemoteAddr(ip);
            return req;
        });
        return builder;
    }

    private static MockMultipartFile photo() {
        return new MockMultipartFile("photo", "litter.jpg", "image/jpeg", TestImages.jpeg(800, 600));
    }
}
