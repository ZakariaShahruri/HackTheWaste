package be.leuven.leuvengo.web;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the full app with demo data and hits every public read endpoint. With
 * open-in-view disabled this also proves the DTO mapping never touches an
 * unloaded lazy association.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PublicApiSmokeTest {

    @Autowired
    MockMvc mvc;

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/stats", "/api/hotspots", "/api/hotspots/active", "/api/segments",
            "/api/faculties", "/api/leaderboard", "/api/work-orders", "/api/rewards", "/api/feed"
    })
    void publicEndpointsRespond(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isOk());
    }

    @Test
    void hotspotsIncludeTheirStreetSegment() throws Exception {
        mvc.perform(get("/api/hotspots"))
                .andExpect(jsonPath("$[*].segmentName", everyItem(notNullValue())));
    }

    @Test
    void segmentsIncludeTheirFaculty() throws Exception {
        mvc.perform(get("/api/segments"))
                .andExpect(jsonPath("$[*].facultyShortCode", everyItem(notNullValue())));
    }
}
