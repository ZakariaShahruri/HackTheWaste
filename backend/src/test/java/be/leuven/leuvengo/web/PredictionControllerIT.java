package be.leuven.leuvengo.web;

import be.leuven.leuvengo.service.PredictionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PredictionControllerIT {

    @Autowired MockMvc mvc;
    @MockitoBean PredictionService predictions;

    @Test
    void asksTheModelForThatWeekdayAndHour() throws Exception {
        when(predictions.predictGrid(any(), anyInt())).thenReturn(Optional.of(
                List.of(new PredictionService.PredictPoint(50.88, 4.70, 0.7))));

        mvc.perform(get("/api/predictions/grid").param("day", "4").param("hour", "22").param("steps", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.points[0].likelihood").value(0.7));

        verify(predictions).predictGrid(
                argThat((LocalDateTime at) -> at.getDayOfWeek() == DayOfWeek.FRIDAY && at.getHour() == 22),
                eq(20));
    }

    @Test
    void clampsTheGridSize() throws Exception {
        when(predictions.predictGrid(any(), anyInt())).thenReturn(Optional.of(List.of()));

        mvc.perform(get("/api/predictions/grid").param("steps", "5000")).andExpect(status().isOk());

        verify(predictions).predictGrid(any(), eq(60));
    }

    @Test
    void saysSoWhenTheModelIsOffline() throws Exception {
        when(predictions.predictGrid(any(), anyInt())).thenReturn(Optional.empty());

        mvc.perform(get("/api/predictions/grid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.points").isEmpty());
    }

    @Test
    void rejectsImpossibleDaysAndHours() throws Exception {
        mvc.perform(get("/api/predictions/grid").param("day", "7")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/predictions/grid").param("hour", "24")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/predictions/grid").param("hour", "-1")).andExpect(status().isBadRequest());
    }
}
