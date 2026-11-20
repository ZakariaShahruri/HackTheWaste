package be.leuven.leuvengo.service;

import be.leuven.leuvengo.domain.Hotspot;
import be.leuven.leuvengo.domain.Report;
import be.leuven.leuvengo.domain.StreetSegment;
import be.leuven.leuvengo.domain.WorkOrder;
import be.leuven.leuvengo.repository.FacultyRepository;
import be.leuven.leuvengo.repository.StreetSegmentRepository;
import be.leuven.leuvengo.repository.WorkOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs reports through the real pipeline (clustering, escalation, scoring,
 * points) against the seeded demo city. Gasthuisberg has no seeded reports,
 * so everything measured here is caused by the test.
 */
@SpringBootTest
@Transactional
class ReportPipelineIT {

    private static final double LAT = 50.8775;
    private static final double LNG = 4.6735;

    @Autowired ReportService reportService;
    @Autowired StreetSegmentRepository segments;
    @Autowired FacultyRepository faculties;
    @Autowired WorkOrderRepository workOrders;
    @Autowired WorkOrderService workOrderService;
    @Autowired Clock clock;
    @Autowired ConfigurableApplicationContext context;

    @Test
    void thirdNearbyReportEscalatesToAPlanonWorkOrder() {
        Report first = submit(10);
        submit(15);
        assertThat(first.getHotspot().getStatus()).isEqualTo(Hotspot.Status.OPEN);

        Report third = submit(5);
        Hotspot hotspot = third.getHotspot();

        assertThat(hotspot).isSameAs(first.getHotspot());
        assertThat(hotspot.getReportCount()).isEqualTo(3);
        assertThat(hotspot.getMeanCleanliness()).isEqualTo(10.0);
        assertThat(hotspot.getStatus()).isEqualTo(Hotspot.Status.DISPATCHED);

        WorkOrder order = workOrders.findAll().stream()
                .filter(w -> w.getHotspot().getId().equals(hotspot.getId()))
                .findFirst().orElseThrow();
        assertThat(order.getStatus()).isEqualTo(WorkOrder.Status.DISPATCHED);
        assertThat(order.getPriority()).isEqualTo(WorkOrder.Priority.URGENT);
        assertThat(order.getPlanonRef()).startsWith("PLN-");
    }

    @Test
    void reportsAtADispatchedSpotJoinItInsteadOfOrderingASecondCrew() {
        submit(10);
        submit(15);
        Hotspot dispatched = submit(5).getHotspot();

        Report fourth = submit(20);

        assertThat(fourth.getHotspot()).isSameAs(dispatched);
        assertThat(dispatched.getReportCount()).isEqualTo(4);
        assertThat(workOrders.findAll())
                .filteredOn(w -> w.getHotspot().getId().equals(dispatched.getId()))
                .hasSize(1);
    }

    @Test
    void dispatchingByHandTwiceOrdersOneCrew() {
        Hotspot hotspot = submit(10).getHotspot();

        Long first = workOrderService.dispatch(hotspot.getId()).id();
        Long second = workOrderService.dispatch(hotspot.getId()).id();

        assertThat(second).isEqualTo(first);
        assertThat(workOrders.findAll())
                .filteredOn(w -> w.getHotspot().getId().equals(hotspot.getId()))
                .hasSize(1);
    }

    @Test
    void backdatedReportsDateTheirHotspotAndWorkOrderToo() {
        Instant first = clock.instant().minus(Duration.ofDays(2));
        Instant third = first.plus(Duration.ofHours(2));

        submitAt(10, first);
        submitAt(15, first.plus(Duration.ofHours(1)));
        Report last = submitAt(5, third);

        Hotspot hotspot = last.getHotspot();
        assertThat(last.getReportedAt()).isEqualTo(third);
        assertThat(hotspot.getCreatedAt()).isEqualTo(first);
        assertThat(hotspot.getLastUpdatedAt()).isEqualTo(third);
        assertThat(workOrders.findAll())
                .filteredOn(w -> w.getHotspot().getId().equals(hotspot.getId()))
                .singleElement()
                .satisfies(w -> {
                    assertThat(w.getCreatedAt()).isEqualTo(third);
                    assertThat(w.getDispatchedAt()).isEqualTo(third);
                });
    }

    @Test
    void filthyReportsLowerTheStreetScore() {
        StreetSegment street = gasthuisberg();
        double before = street.getAiCleanlinessScore();

        submit(0);

        assertThat(gasthuisberg().getAiCleanlinessScore()).isLessThan(before);
        assertThat(gasthuisberg().getRecentReportCount()).isEqualTo(1);
    }

    @Test
    void startupRescoresStreetsThatAgedWhileTheAppSlept() {
        gasthuisberg().setAiCleanlinessScore(0.0);

        context.publishEvent(new ApplicationReadyEvent(
                new SpringApplication(), new String[0], context, Duration.ZERO));

        assertThat(gasthuisberg().getAiCleanlinessScore())
                .isEqualTo(SegmentScorer.score(List.of(), clock.instant()));
    }

    @Test
    void reportsCreditTheChosenClanByPointsPolicy() {
        int before = faculties.findByShortCode("FAR").orElseThrow().getPoints();

        submit(0);

        assertThat(faculties.findByShortCode("FAR").orElseThrow().getPoints())
                .isEqualTo(before + PointsPolicy.pointsFor(0));
    }

    private Report submit(int score) {
        return reportService.submit(submission(score));
    }

    private Report submitAt(int score, Instant at) {
        return reportService.submit(submission(score), at);
    }

    private static ReportService.Submission submission(int score) {
        return new ReportService.Submission(
                LAT, LNG, score, Report.Source.WEB, Reporter.anonymous("test-reporter", "FAR"), ReportService.Evidence.NONE);
    }

    @Test
    void reportsOutsideLeuvenAreRefused() {
        assertThatThrownBy(() -> reportService.submit(new ReportService.Submission(
                51.2194, 4.4025, 50, Report.Source.WEB, Reporter.anonymous("antwerp", null), ReportService.Evidence.NONE)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outside Leuven");
    }

    private StreetSegment gasthuisberg() {
        return segments.findAll().stream()
                .filter(s -> s.getName().equals("Gasthuisberg"))
                .findFirst().orElseThrow();
    }
}
