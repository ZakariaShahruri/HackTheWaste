package be.leuven.leuvengo.photo;

import be.leuven.leuvengo.support.TestImages;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PhotoEndpointIT {

    @Autowired MockMvc mvc;
    @Autowired PhotoStorage storage;
    @Autowired ImageNormalizer images;

    @Test
    void storesInTheDatabaseByDefaultAndServesWithLongCaching() throws Exception {
        ImageNormalizer.Normalized photo = images.normalize(TestImages.jpeg(320, 240));

        String url = storage.store(photo, "wa-test");

        assertThat(storage).isInstanceOf(DatabasePhotoStorage.class);
        assertThat(url).matches("/api/photos/[0-9a-f-]{36}");
        mvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(photo.bytes()))
                .andExpect(header().string("Cache-Control", "max-age=31536000, public, immutable"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void unknownOrMalformedIdsAre404() throws Exception {
        mvc.perform(get("/api/photos/00000000-0000-0000-0000-000000000000")).andExpect(status().isNotFound());
        mvc.perform(get("/api/photos/../../etc")).andExpect(status().isNotFound());
        mvc.perform(get("/api/photos/not-a-uuid")).andExpect(status().isNotFound());
    }
}
