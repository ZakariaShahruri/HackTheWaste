package be.leuven.leuvengo.web;

import be.leuven.leuvengo.domain.Report;
import be.leuven.leuvengo.repository.ReportRepository;
import be.leuven.leuvengo.service.GeminiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Regression: WhatsApp used to store a filthy photo as a "clean" report and vice versa. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WhatsAppRatingIT {

    @Autowired MockMvc mvc;
    @Autowired ReportRepository reports;
    @MockitoBean GeminiService gemini;

    @Test
    void aFilthyPhotoIsStoredAsADirtyReport() throws Exception {
        when(gemini.scorePhoto(anyString())).thenReturn(
                new GeminiService.ScoreResult(false, null, 10, "Overflowing bin", List.of("plastic")));

        mvc.perform(post("/api/whatsapp/webhook")
                        .param("From", "whatsapp:+32470000000")
                        .param("Latitude", "50.8525").param("Longitude", "4.6720"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/whatsapp/webhook")
                        .param("From", "whatsapp:+32470000000")
                        .param("MediaUrl0", "https://api.twilio.com/media/1"))
                .andExpect(status().isOk());

        Report latest = reports.findAll().stream()
                .max(Comparator.comparing(Report::getId)).orElseThrow();
        assertThat(latest.getCleanlinessScore()).isEqualTo(10);
        assertThat(latest.getHotspot().getMeanCleanliness()).isEqualTo(10.0);
    }
}
