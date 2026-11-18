package be.leuven.leuvengo.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class CleanlinessScaleTest {

    @ParameterizedTest
    @CsvSource({"0,0", "9,0", "10,1", "49,2", "50,3", "89,4", "90,5", "100,5", "-20,0", "250,5"})
    void scoreMapsToRatingWhereZeroIsFilthy(int score, int rating) {
        assertThat(CleanlinessScale.toRating(score)).isEqualTo(rating);
    }

    @ParameterizedTest
    @CsvSource({"0,0", "1,20", "3,60", "5,100", "9,100", "-1,0"})
    void ratingMapsBackToScore(int rating, int score) {
        assertThat(CleanlinessScale.fromRating(rating)).isEqualTo(score);
    }

    @Test
    void dirtierScoresGetWorseLabels() {
        assertThat(CleanlinessScale.label(95)).isEqualTo("Clean");
        assertThat(CleanlinessScale.label(65)).isEqualTo("Mild litter");
        assertThat(CleanlinessScale.label(45)).isEqualTo("Moderate waste");
        assertThat(CleanlinessScale.label(5)).isEqualTo("Critical");
    }
}
