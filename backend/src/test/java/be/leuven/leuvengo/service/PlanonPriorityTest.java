package be.leuven.leuvengo.service;

import be.leuven.leuvengo.domain.WorkOrder.Priority;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PlanonPriorityTest {

    @ParameterizedTest
    @CsvSource({
            "10, 3, URGENT", "35, 3, HIGH", "55, 3, MEDIUM", "80, 3, LOW",
            "80, 4, HIGH", "80, 6, URGENT"
    })
    void dirtierOrBusierHotspotsAreMoreUrgent(double cleanliness, int reports, Priority expected) {
        assertThat(PlanonService.priorityFor(cleanliness, reports)).isEqualTo(expected);
    }
}
