package be.leuven.leuvengo.service;

import be.leuven.leuvengo.config.LeuvenGoProperties;
import be.leuven.leuvengo.domain.Hotspot;
import be.leuven.leuvengo.domain.Report;
import be.leuven.leuvengo.domain.StreetSegment;
import be.leuven.leuvengo.repository.HotspotRepository;
import be.leuven.leuvengo.repository.StreetSegmentRepository;
import be.leuven.leuvengo.support.TestProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrafficServiceTest {

    /** ~0.00009° latitude is ~10 m. */
    private static final double TEN_METERS_LAT = 0.00009;
    private static final double LAT = 50.8790;
    private static final double LNG = 4.7010;

    private HotspotRepository hotspots;
    private TrafficService traffic;

    @BeforeEach
    void setUp() {
        hotspots = mock(HotspotRepository.class);
        StreetSegmentRepository segments = mock(StreetSegmentRepository.class);
        when(hotspots.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(segments.findAll()).thenReturn(List.of(
                segment("Oude Markt", LAT, LNG),
                segment("Station", 50.8810, 4.7160)));
        LeuvenGoProperties props = TestProperties.with(Map.of("traffic.cluster-radius-m", "40"));
        traffic = new TrafficService(hotspots, segments, props);
    }

    @Test
    void firstReportOpensAHotspotNamedAfterTheNearestStreet() {
        when(hotspots.findAllByStatusIn(anyCollection())).thenReturn(List.of());

        Hotspot h = traffic.ingest(report(LAT + TEN_METERS_LAT, LNG, 30));

        assertThat(h.getStatus()).isEqualTo(Hotspot.Status.OPEN);
        assertThat(h.getReportCount()).isEqualTo(1);
        assertThat(h.getMeanCleanliness()).isEqualTo(30);
        assertThat(h.getLabel()).isEqualTo("Oude Markt");
    }

    @Test
    void reportWithinRadiusMergesAndAveragesPositionAndCleanliness() {
        Hotspot existing = hotspot(LAT, LNG, 20.0, 1);
        when(hotspots.findAllByStatusIn(anyCollection())).thenReturn(List.of(existing));

        Report r = report(LAT + 2 * TEN_METERS_LAT, LNG, 60);
        Hotspot merged = traffic.ingest(r);

        assertThat(merged).isSameAs(existing);
        assertThat(merged.getReportCount()).isEqualTo(2);
        assertThat(merged.getMeanCleanliness()).isEqualTo(40);
        assertThat(merged.getCenterLat()).isCloseTo(LAT + TEN_METERS_LAT, within(1e-9));
        assertThat(r.getHotspot()).isSameAs(existing);
    }

    @Test
    void reportOutsideRadiusOpensANewHotspot() {
        Hotspot existing = hotspot(LAT, LNG, 20.0, 1);
        when(hotspots.findAllByStatusIn(anyCollection())).thenReturn(List.of(existing));

        Hotspot h = traffic.ingest(report(LAT + 6 * TEN_METERS_LAT, LNG, 60));

        assertThat(h).isNotSameAs(existing);
        assertThat(existing.getReportCount()).isEqualTo(1);
    }

    @Test
    void reportJoinsTheClosestOfSeveralCandidates() {
        Hotspot far = hotspot(LAT + 3 * TEN_METERS_LAT, LNG, 50.0, 1);
        Hotspot near = hotspot(LAT + TEN_METERS_LAT, LNG, 50.0, 1);
        when(hotspots.findAllByStatusIn(anyCollection())).thenReturn(List.of(far, near));

        assertThat(traffic.ingest(report(LAT, LNG, 50))).isSameAs(near);
    }

    private static Report report(double lat, double lng, int score) {
        Report r = new Report();
        r.setLat(lat);
        r.setLng(lng);
        r.setCleanlinessScore(score);
        r.setReportedAt(Instant.now().minus(Duration.ofMinutes(1)));
        return r;
    }

    private static Hotspot hotspot(double lat, double lng, double cleanliness, int count) {
        Hotspot h = new Hotspot();
        h.setCenterLat(lat);
        h.setCenterLng(lng);
        h.setMeanCleanliness(cleanliness);
        h.setReportCount(count);
        h.setStatus(Hotspot.Status.OPEN);
        return h;
    }

    private static StreetSegment segment(String name, double lat, double lng) {
        StreetSegment s = new StreetSegment();
        s.setName(name);
        s.setCenterLat(lat);
        s.setCenterLng(lng);
        return s;
    }
}
