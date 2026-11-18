package be.leuven.leuvengo.web;

import be.leuven.leuvengo.domain.Report;
import be.leuven.leuvengo.service.ReportService;
import be.leuven.leuvengo.service.Reporter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FeedPrivacyIT {

    @Autowired MockMvc mvc;
    @Autowired ReportService reports;

    @Test
    void theFeedShowsPhotoReportsWithoutPersonalDataAndOnlyApproximateLocations() throws Exception {
        reports.submit(new ReportService.Submission(50.8765432, 4.7012345, 30, Report.Source.WHATSAPP,
                Reporter.anonymous("wa-0123456789ab", "EKO"),
                new ReportService.Evidence("/api/photos/x", List.of("plastic"), "Overflowing bin", null)));

        mvc.perform(get("/api/feed").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("email"))))
                .andExpect(content().string(not(containsString("wa-0123456789ab"))))
                .andExpect(jsonPath("$.content[0].username").value("WhatsApp reporter"))
                .andExpect(jsonPath("$.content[0].facultyShortCode").value("EKO"))
                .andExpect(jsonPath("$.content[0].description").value("Overflowing bin"))
                .andExpect(jsonPath("$.content[0].userRating").value(2))
                .andExpect(jsonPath("$.content[0].lat").value(50.8765))
                .andExpect(jsonPath("$.content[0].lng").value(4.7012));
    }

    @Test
    void reportsWithoutAPhotoStayOffTheFeed() throws Exception {
        reports.submit(new ReportService.Submission(50.8765, 4.7012, 30, Report.Source.WEB,
                Reporter.anonymous("no-photo", null), ReportService.Evidence.NONE));

        mvc.perform(get("/api/feed").param("size", "48"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Web reporter"))));
    }

    @Test
    void theClanFilterOnlyShowsThatClan() throws Exception {
        reports.submit(new ReportService.Submission(50.8765, 4.7012, 30, Report.Source.WHATSAPP,
                Reporter.anonymous("x", "APO"), new ReportService.Evidence("/api/photos/y", List.of(), null, null)));

        mvc.perform(get("/api/feed").param("clan", "PSY"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("\"APO\""))));
        mvc.perform(get("/api/feed").param("clan", "APO"))
                .andExpect(jsonPath("$.content[0].facultyShortCode").value("APO"));
    }
}
