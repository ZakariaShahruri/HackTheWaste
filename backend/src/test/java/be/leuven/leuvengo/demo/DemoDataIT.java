package be.leuven.leuvengo.demo;

import be.leuven.leuvengo.domain.Faculty;
import be.leuven.leuvengo.domain.Report;
import be.leuven.leuvengo.domain.StreetSegment;
import be.leuven.leuvengo.domain.WorkOrder;
import be.leuven.leuvengo.photo.ImageNormalizer;
import be.leuven.leuvengo.photo.PhotoStorage;
import be.leuven.leuvengo.photo.StoredPhotoRepository;
import be.leuven.leuvengo.repository.AppUserRepository;
import be.leuven.leuvengo.repository.FacultyRepository;
import be.leuven.leuvengo.repository.HotspotRepository;
import be.leuven.leuvengo.repository.ReportRepository;
import be.leuven.leuvengo.repository.RewardItemRepository;
import be.leuven.leuvengo.repository.StreetSegmentRepository;
import be.leuven.leuvengo.repository.WorkOrderRepository;
import be.leuven.leuvengo.service.PointsPolicy;
import be.leuven.leuvengo.service.ReportService;
import be.leuven.leuvengo.service.Reporter;
import be.leuven.leuvengo.service.SegmentScorer;
import be.leuven.leuvengo.service.StatsService;
import be.leuven.leuvengo.support.TestImages;
import be.leuven.leuvengo.web.dto.Dtos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** What the dashboards show right after a fresh start, before anyone has reported anything. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class DemoDataIT {

    @Autowired MockMvc mvc;
    @Autowired DemoDataSeeder seeder;
    @Autowired FacultyRepository faculties;
    @Autowired StreetSegmentRepository segments;
    @Autowired RewardItemRepository rewards;
    @Autowired ReportRepository reports;
    @Autowired HotspotRepository hotspots;
    @Autowired WorkOrderRepository workOrders;
    @Autowired AppUserRepository users;
    @Autowired StatsService stats;
    @Autowired JsonMapper json;
    @Autowired DemoReset reset;
    @Autowired ReportService reportService;
    @Autowired PhotoStorage photoStorage;
    @Autowired ImageNormalizer images;
    @Autowired StoredPhotoRepository storedPhotos;

    @Test
    void cleanupCrewsHaveWorkOrdersInEveryState() {
        assertThat(workOrders.findAll())
                .hasSizeBetween(6, 8)
                .extracting(WorkOrder::getStatus)
                .contains(WorkOrder.Status.DISPATCHED, WorkOrder.Status.COMPLETED);
    }

    @Test
    void streetScoresRangeFromFilthyToCleanerThanThePrior() {
        List<Double> scores = segments.findAll().stream().map(StreetSegment::getAiCleanlinessScore).toList();

        assertThat(scores).anySatisfy(s -> assertThat(s).isLessThan(40));
        assertThat(scores).anySatisfy(s -> assertThat(s).isGreaterThan(SegmentScorer.PRIOR_SCORE));
    }

    @Test
    void theWeeklyTrendHasUpsAndDowns() {
        List<Long> counts = stats.snapshot().reportsLast7Days().stream().map(Dtos.TimeBucket::count).toList();

        assertThat(counts).filteredOn(c -> c > 0).hasSizeGreaterThanOrEqualTo(5);
        assertThat(counts.stream().distinct()).hasSizeGreaterThan(2);
    }

    @Test
    @Transactional
    void everyStreetCountsForTheClanWhoseTerritoryItLiesIn() {
        assertThat(segments.findAll()).allSatisfy(street -> assertThat(
                territoryContains(street.getFaculty().getTerritoryGeoJson(), street.getCenterLat(), street.getCenterLng()))
                .as("%s in %s", street.getName(), street.getFaculty().getShortCode())
                .isTrue());
    }

    @Test
    void theFeedOpensWithPhotosThatLoad() throws Exception {
        mvc.perform(get("/api/feed"))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(DemoActivity.PHOTO_REPORTS)))
                .andExpect(jsonPath("$.content[0].username").value("Leuven Go demo"));

        String photoUrl = reports.findByPhotoUrlIsNotNullOrderByReportedAtDesc(
                PageRequest.of(0, 1)).getContent().getFirst().getPhotoUrl();
        mvc.perform(get(photoUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"));
    }

    @Test
    void seedingAgainAddsNothing() {
        List<Long> before = counts();

        seeder.afterSingletonsInstantiated();

        assertThat(counts()).isEqualTo(before);
    }

    @Test
    void theNightlyResetClearsUploadsAndStartsAFreshWeek() throws Exception {
        List<Long> fresh = counts();
        String upload = photoStorage.store(images.normalize(TestImages.jpeg(64, 48)), "web-test");
        reportService.submit(new ReportService.Submission(50.8775, 4.6735, 10, Report.Source.WEB,
                Reporter.anonymous("web-test", "PSY"), new ReportService.Evidence(upload, List.of(), null, null)));

        reset.reset();

        assertThat(counts()).isEqualTo(fresh);
        assertThat(reports.findAllPhotoUrls())
                .doesNotContain(upload)
                .hasSize((int) storedPhotos.count());
        assertThat(faculties.findAll().stream().mapToInt(Faculty::getPoints).sum())
                .isEqualTo(reports.findAll().stream().mapToInt(r -> PointsPolicy.pointsFor(r.getCleanlinessScore())).sum());
    }

    private List<Long> counts() {
        return List.of(faculties.count(), segments.count(), rewards.count(), reports.count(),
                hotspots.count(), workOrders.count(), users.count());
    }

    /** Checks against the stored GeoJSON (what the map draws), not the seeder's own data. */
    private boolean territoryContains(String geoJson, double lat, double lng) {
        List<JsonNode> ring = StreamSupport.stream(json.readTree(geoJson).at("/coordinates/0").spliterator(), false)
                .toList();
        double lngMin = ring.stream().mapToDouble(p -> p.get(0).asDouble()).min().orElseThrow();
        double lngMax = ring.stream().mapToDouble(p -> p.get(0).asDouble()).max().orElseThrow();
        double latMin = ring.stream().mapToDouble(p -> p.get(1).asDouble()).min().orElseThrow();
        double latMax = ring.stream().mapToDouble(p -> p.get(1).asDouble()).max().orElseThrow();
        return lat >= latMin && lat <= latMax && lng >= lngMin && lng <= lngMax;
    }
}
