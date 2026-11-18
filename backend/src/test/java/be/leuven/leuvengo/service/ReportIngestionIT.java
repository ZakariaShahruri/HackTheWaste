package be.leuven.leuvengo.service;

import be.leuven.leuvengo.domain.Report;
import be.leuven.leuvengo.photo.StoredPhotoRepository;
import be.leuven.leuvengo.repository.ReportRepository;
import be.leuven.leuvengo.service.ReportIngestionService.Filed;
import be.leuven.leuvengo.service.ReportIngestionService.Reason;
import be.leuven.leuvengo.service.ReportIngestionService.Rejected;
import be.leuven.leuvengo.support.TestImages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Not @Transactional: these tests check what the service itself commits or rolls back.
 * Other tests only compare counts against a baseline, so the committed rows are harmless.
 */
@SpringBootTest
class ReportIngestionIT {

    private static final double LAT = 50.8798, LNG = 4.7005;

    @Autowired ReportIngestionService ingestion;
    @Autowired ReportRepository reports;
    @Autowired StoredPhotoRepository photos;
    @MockitoBean GeminiService gemini;
    @MockitoSpyBean ReportService reportService;

    private long reportsBefore;
    private long photosBefore;

    @BeforeEach
    void setUp() {
        reportsBefore = reports.count();
        photosBefore = photos.count();
        when(gemini.scorePhoto(any(), anyString())).thenReturn(
                GeminiService.ScoreResult.genuine(25, "Bags piled next to a bin", List.of("plastic", "food_waste")));
    }

    @Test
    void aGenuinePhotoIsStoredScoredAndFiledTogether() {
        var outcome = ingestion.ingest(report(LAT, LNG, TestImages.jpeg(2000, 1500)));

        Filed filed = (Filed) outcome;
        Report report = reports.findById(filed.reportId()).orElseThrow();
        assertThat(report.getPhotoUrl()).isEqualTo(filed.photoUrl()).startsWith("/api/photos/");
        assertThat(report.getCleanlinessScore()).isEqualTo(25);
        assertThat(report.getSignalTags()).isEqualTo("plastic,food_waste");
        assertThat(report.getAiDescription()).isEqualTo("Bags piled next to a bin");
        assertThat(report.getSource()).isEqualTo(Report.Source.WEB);
        assertThat(photos.count()).isEqualTo(photosBefore + 1);
        assertThat(filed.hotspotId()).isPositive();
    }

    @Test
    void photosOutsideLeuvenAreRejectedBeforeAskingTheAi() {
        var outcome = ingestion.ingest(report(51.2194, 4.4025, TestImages.jpeg(100, 100)));

        assertThat(outcome).isEqualTo(new Rejected(Reason.OUTSIDE_LEUVEN, null));
        verify(gemini, never()).scorePhoto(any(), anyString());
    }

    @Test
    void unreadableFilesAreRejected() {
        var outcome = ingestion.ingest(report(LAT, LNG, "not an image".getBytes()));

        assertThat(((Rejected) outcome).reason()).isEqualTo(Reason.UNREADABLE_IMAGE);
        verify(gemini, never()).scorePhoto(any(), anyString());
    }

    @Test
    void fakePhotosAreNeitherStoredNorFiled() {
        when(gemini.scorePhoto(any(), anyString())).thenReturn(GeminiService.ScoreResult.rejected("Screenshot"));

        var outcome = ingestion.ingest(report(LAT, LNG, TestImages.jpeg(100, 100)));

        assertThat(outcome).isEqualTo(new Rejected(Reason.NOT_GENUINE, "Screenshot"));
        assertThat(reports.count()).isEqualTo(reportsBefore);
        assertThat(photos.count()).isEqualTo(photosBefore);
    }

    @Test
    void ifFilingFailsThePhotoIsNotKeptEither() {
        doThrow(new IllegalStateException("db down")).when(reportService).submit(any());

        assertThatThrownBy(() -> ingestion.ingest(report(LAT, LNG, TestImages.jpeg(100, 100))))
                .hasMessage("db down");
        assertThat(photos.count()).isEqualTo(photosBefore);
    }

    private static ReportIngestionService.PhotoReport report(double lat, double lng, byte[] image) {
        return new ReportIngestionService.PhotoReport(lat, lng, image, Report.Source.WEB,
                Reporter.anonymous("ingest-test", null));
    }
}
