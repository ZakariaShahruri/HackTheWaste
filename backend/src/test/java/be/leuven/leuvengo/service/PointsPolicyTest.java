package be.leuven.leuvengo.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PointsPolicyTest {

    @ParameterizedTest
    @CsvSource({"100,10", "85,17", "50,35", "19,50", "0,60", "-5,60", "130,10"})
    void dirtierReportsEarnMore(int score, int points) {
        assertThat(PointsPolicy.pointsFor(score)).isEqualTo(points);
    }
}
