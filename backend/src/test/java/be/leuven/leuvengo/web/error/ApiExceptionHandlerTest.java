package be.leuven.leuvengo.web.error;

import be.leuven.leuvengo.config.ClockConfig;
import be.leuven.leuvengo.config.LeuvenGoProperties;
import be.leuven.leuvengo.security.AuthService;
import be.leuven.leuvengo.security.RateLimiter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ApiExceptionHandlerTest.ThrowingController.class)
@Import({ApiExceptionHandlerTest.ThrowingController.class, ApiExceptionHandlerTest.PropertiesConfig.class,
        RateLimiter.class, ClockConfig.class})
class ApiExceptionHandlerTest {

    @Autowired
    MockMvc mvc;

    /** Needed by the @CurrentUser resolver that the web slice picks up. */
    @MockitoBean
    AuthService auth;

    @Test
    void notFoundExceptionBecomes404WithMessage() throws Exception {
        mvc.perform(get("/test/missing/7"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Thing 7 not found"))
                .andExpect(jsonPath("$.path").value("/test/missing/7"));
    }

    @Test
    void illegalArgumentBecomes400() throws Exception {
        mvc.perform(get("/test/illegal"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("bad input"));
    }

    @Test
    void validationErrorsListTheFields() throws Exception {
        mvc.perform(post("/test/validated").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("name: must not be blank"));
    }

    @Test
    void malformedJsonBecomes400() throws Exception {
        mvc.perform(post("/test/validated").contentType(MediaType.APPLICATION_JSON).content("{nope"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void typeMismatchBecomes400() throws Exception {
        mvc.perform(get("/test/missing/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for 'id'"));
    }

    @Test
    void unexpectedErrorsDoNotLeakTheirMessage() throws Exception {
        mvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Something went wrong on our side"));
    }

    @Test
    void unsupportedMethodKeepsItsStatus() throws Exception {
        mvc.perform(post("/test/boom"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @TestConfiguration
    @EnableConfigurationProperties(LeuvenGoProperties.class)
    static class PropertiesConfig {}

    record NamedThing(@NotBlank String name) {}

    @RestController
    static class ThrowingController {

        @GetMapping("/test/missing/{id}")
        String missing(@PathVariable Long id) {
            throw new NotFoundException("Thing", id);
        }

        @GetMapping("/test/illegal")
        String illegal() {
            throw new IllegalArgumentException("bad input");
        }

        @PostMapping("/test/validated")
        String validated(@Valid @RequestBody NamedThing body) {
            return body.name();
        }

        @GetMapping("/test/boom")
        String boom() {
            throw new IllegalStateException("secret internal detail");
        }
    }
}
