package be.leuven.leuvengo;

import be.leuven.leuvengo.photo.ImageNormalizer;
import be.leuven.leuvengo.photo.PhotoStorage;
import be.leuven.leuvengo.repository.FacultyRepository;
import be.leuven.leuvengo.support.TestImages;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the app on a real Postgres, as in prod: Flyway runs the migrations,
 * Hibernate validates every entity against them and the demo city is seeded.
 * Skipped when Docker isn't available.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class PostgresIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired MockMvc mvc;
    @Autowired FacultyRepository faculties;
    @Autowired PhotoStorage storage;
    @Autowired ImageNormalizer images;

    @Test
    void migratesAndSeedsTheDemoCity() throws Exception {
        assertThat(faculties.count()).isPositive();
        mvc.perform(get("/api/stats")).andExpect(status().isOk());
        mvc.perform(get("/api/faculties")).andExpect(status().isOk());
    }

    @Test
    void photosRoundTripThroughBytea() throws Exception {
        ImageNormalizer.Normalized photo = images.normalize(TestImages.jpeg(320, 240));

        mvc.perform(get(storage.store(photo, "pg-test")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(photo.bytes()));
    }
}
