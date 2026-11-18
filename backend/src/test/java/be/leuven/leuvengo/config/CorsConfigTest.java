package be.leuven.leuvengo.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "leuvengo.cors.allowed-origins=https://a.example, https://b.example")
@AutoConfigureMockMvc
class CorsConfigTest {

    @Autowired
    MockMvc mvc;

    @Test
    void originsAreTrimmed() throws Exception {
        mvc.perform(options("/api/stats")
                        .header("Origin", "https://b.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://b.example"));
    }

    @Test
    void unknownOriginsAreRejected() throws Exception {
        mvc.perform(options("/api/stats")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
