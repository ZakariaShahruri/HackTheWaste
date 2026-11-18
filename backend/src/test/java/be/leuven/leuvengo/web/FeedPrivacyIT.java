package be.leuven.leuvengo.web;

import be.leuven.leuvengo.domain.TrashPhoto;
import be.leuven.leuvengo.repository.TrashPhotoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FeedPrivacyIT {

    @Autowired MockMvc mvc;
    @Autowired TrashPhotoRepository photos;

    @Test
    void theFeedHasNoPersonalDataAndOnlyApproximateLocations() throws Exception {
        TrashPhoto photo = new TrashPhoto();
        photo.setPhotoUrl("https://example.test/p.jpg");
        photo.setLat(50.8765432);
        photo.setLng(4.7012345);
        photo.setCleanlinessScore(30);
        photo.setReportedAt(Instant.now().plusSeconds(60));
        photos.save(photo);

        mvc.perform(get("/api/feed").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("email"))))
                .andExpect(jsonPath("$.content[0].username").value("Anonymous Leuvenaar"))
                .andExpect(jsonPath("$.content[0].lat").value(50.8765))
                .andExpect(jsonPath("$.content[0].lng").value(4.7012));
    }
}
