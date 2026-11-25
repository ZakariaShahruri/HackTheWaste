package be.leuven.leuvengo.whatsapp;

import be.leuven.leuvengo.net.SafeHttpFetcher;
import be.leuven.leuvengo.repository.PendingReportRepository;
import be.leuven.leuvengo.repository.ReportRepository;
import be.leuven.leuvengo.service.GeminiService;
import be.leuven.leuvengo.support.TestImages;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** A cold start releases everything a sender sent meanwhile at once. Not transactional, so the threads really race. */
@SpringBootTest
class WhatsAppConcurrencyIT {

    @Autowired WhatsAppBot bot;
    @Autowired ReportRepository reports;
    @Autowired PendingReportRepository pending;
    @MockitoBean GeminiService gemini;
    @MockitoBean TwilioMediaFetcher media;

    @Test
    void aPinAndPhotoSentTogetherBecomeOneReport() throws Exception {
        when(media.fetch(anyString())).thenReturn(new SafeHttpFetcher.Download(TestImages.jpeg(64, 48), "image/jpeg"));
        when(gemini.scorePhoto(any(), anyString())).thenReturn(
                GeminiService.ScoreResult.genuine(10, "Overflowing bin", List.of()));
        int rounds = 20;
        long before = reports.count();

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            for (int i = 0; i < rounds; i++) {
                String from = "whatsapp:+3247000%04d".formatted(i);
                CyclicBarrier together = new CyclicBarrier(2);
                Future<String> pin = pool.submit(() -> {
                    together.await();
                    return bot.handle(new WhatsAppBot.Inbound(from, null, null, 50.8525, 4.6720, null, null));
                });
                Future<String> photo = pool.submit(() -> {
                    together.await();
                    return bot.handle(new WhatsAppBot.Inbound(
                            from, null, null, null, null, "https://api.twilio.com/media/1", "image/jpeg"));
                });
                assertThat(pin.get() + photo.get()).contains("10/100");
            }
        }

        assertThat(reports.count()).isEqualTo(before + rounds);
        assertThat(pending.count()).isZero();
    }
}
