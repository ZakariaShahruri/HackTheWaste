package be.leuven.leuvengo.demo;

import be.leuven.leuvengo.service.GeoBox;
import be.leuven.leuvengo.service.LeuvenArea;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DemoCityTest {

    @Test
    void territoriesNeverOverlap() {
        for (DemoCity.Clan a : DemoCity.CLANS) {
            for (DemoCity.Clan b : DemoCity.CLANS) {
                if (a != b) {
                    assertThat(overlap(a.territory(), b.territory()))
                            .as("%s and %s overlap", a.code(), b.code()).isFalse();
                }
            }
        }
    }

    @Test
    void everyStreetIsInLeuvenAndOnSomeClansTurf() {
        assertThat(DemoCity.STREETS).allSatisfy(street -> {
            assertThat(LeuvenArea.contains(street.lat(), street.lng())).isTrue();
            assertThat(DemoCity.clanAt(street.lat(), street.lng())).as(street.name()).isPresent();
        });
    }

    @Test
    void clanAtFindsTheTerritoryAPointIsIn() {
        assertThat(DemoCity.clanAt(50.8785, 4.7000)).map(DemoCity.Clan::code).hasValue("TMO");
        assertThat(DemoCity.clanAt(50.8640, 4.6790)).map(DemoCity.Clan::code).hasValue("FAR");
        assertThat(DemoCity.clanAt(50.95, 4.70)).isEmpty();
    }

    @Test
    void territoryGeoJsonIsAClosedLngLatRing() {
        assertThat(DemoCity.geoJson(new GeoBox(50.1, 50.2, 4.6, 4.7))).isEqualTo(
                "{\"type\":\"Polygon\",\"coordinates\":[[[4.6,50.1],[4.6,50.2],[4.7,50.2],[4.7,50.1],[4.6,50.1]]]}");
    }

    private static boolean overlap(GeoBox a, GeoBox b) {
        return a.latMin() < b.latMax() && b.latMin() < a.latMax()
                && a.lngMin() < b.lngMax() && b.lngMin() < a.lngMax();
    }
}
