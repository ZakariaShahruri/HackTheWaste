package be.leuven.leuvengo.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PointsPolicyTest {

    @ParameterizedTest
    @CsvSource({"100,1", "85,1", "80,2", "50,3", "19,5", "0,6"})
    void dirtierReportsEarnMore(int score, int points) {
        assertThat(PointsPolicy.pointsFor(score)).isEqualTo(points);
    }
}
