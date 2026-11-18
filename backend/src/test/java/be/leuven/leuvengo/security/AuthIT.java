package be.leuven.leuvengo.security;

import be.leuven.leuvengo.domain.AppUser;
import be.leuven.leuvengo.domain.Hotspot;
import be.leuven.leuvengo.domain.WorkOrder;
import be.leuven.leuvengo.repository.AppUserRepository;
import be.leuven.leuvengo.repository.WorkOrderRepository;
import be.leuven.leuvengo.service.ReportService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthIT {

    /** Each test gets its own client IP so rate-limit budgets don't leak between tests. */
    private static final AtomicInteger IP = new AtomicInteger(1);

    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;
    @Autowired ReportService reports;
    @Autowired WorkOrderRepository workOrders;

    private final String ip = "10.0.0." + IP.getAndIncrement();

    @Test
    void registerReturnsATokenWeOnlyStoreHashed() throws Exception {
        String token = register("Ana@Example.be", "secret-pass", "EKO");

        AppUser stored = users.findByEmail("ana@example.be").orElseThrow();
        assertThat(stored.getSessionTokenHash()).isEqualTo(SessionTokens.hash(token));
        assertThat(stored.getSessionTokenHash()).isNotEqualTo(token);

        mvc.perform(withIp(get("/api/auth/me")).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@example.be"))
                .andExpect(jsonPath("$.facultyShortCode").value("EKO"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.token").value(nullValue()));
    }

    @Test
    void rejectsInvalidRegistrations() throws Exception {
        expectRegister400("not-an-email", "secret-pass", null, true, "email");
        expectRegister400("a@b.be", "short", null, true, "password");
        expectRegister400("a@b.be", "x".repeat(73), null, true, "password");
        expectRegister400("a@b.be", "secret-pass", "NOPE", true, "Unknown clan NOPE");
        expectRegister400("a@b.be", "secret-pass", null, false, "consent");
    }

    @Test
    void missingConsentIsAValidationErrorNotAParseError() throws Exception {
        mvc.perform(withIp(post("/api/auth/register")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"c@b.be\",\"password\":\"secret-pass\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("consent")));
    }

    @Test
    void duplicateEmailIsAConflict() throws Exception {
        register("dup@example.be", "secret-pass", null);
        mvc.perform(withIp(post("/api/auth/register")).contentType(MediaType.APPLICATION_JSON)
                        .content(json("DUP@example.be", "secret-pass", null, true)))
                .andExpect(status().isConflict());
    }

    @Test
    void wrongPasswordAndUnknownEmailLookTheSame() throws Exception {
        register("bob@example.be", "secret-pass", null);
        login("bob@example.be", "wrong-pass")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
        login("nobody@example.be", "wrong-pass")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void expiredSessionsAreRejected() throws Exception {
        String token = register("old@example.be", "secret-pass", null);
        AppUser user = users.findByEmail("old@example.be").orElseThrow();
        user.setSessionExpiresAt(Instant.now().minusSeconds(1));

        mvc.perform(withIp(get("/api/auth/me")).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutKillsTheToken() throws Exception {
        String token = register("bye@example.be", "secret-pass", null);
        mvc.perform(withIp(post("/api/auth/logout")).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(withIp(get("/api/auth/me")).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void onlyCrewCanCompleteWorkOrders() throws Exception {
        WorkOrder order = dispatchedOrder();
        String citizen = register("citizen@example.be", "secret-pass", null);
        String crew = tokenFrom(login("crew@leuvengo.demo", "cleanleuven"));

        mvc.perform(withIp(post("/api/work-orders/" + order.getId() + "/complete")))
                .andExpect(status().isUnauthorized());
        mvc.perform(withIp(post("/api/work-orders/" + order.getId() + "/complete"))
                        .header("Authorization", "Bearer " + citizen))
                .andExpect(status().isForbidden());
        mvc.perform(withIp(post("/api/work-orders/" + order.getId() + "/complete"))
                        .header("Authorization", "Bearer " + crew))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        assertThat(order.getHotspot().getStatus()).isEqualTo(Hotspot.Status.RESOLVED);

        mvc.perform(withIp(post("/api/work-orders/999999/complete")).header("Authorization", "Bearer " + crew))
                .andExpect(status().isNotFound());
    }

    @Test
    void loginIsRateLimitedPerClient() throws Exception {
        for (int i = 0; i < 10; i++) {
            login("nobody@example.be", "wrong-pass").andExpect(status().isUnauthorized());
        }
        login("nobody@example.be", "wrong-pass")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    private WorkOrder dispatchedOrder() {
        for (int i = 0; i < 3; i++) {
            reports.submit(new ReportService.Submission(50.8525, 4.6720, 10, null, null, null, null, "t"));
        }
        return workOrders.findAll().stream()
                .filter(w -> w.getStatus() == WorkOrder.Status.DISPATCHED)
                .reduce((a, b) -> b).orElseThrow();
    }

    private String register(String email, String password, String clan) throws Exception {
        String body = mvc.perform(withIp(post("/api/auth/register")).contentType(MediaType.APPLICATION_JSON)
                        .content(json(email, password, clan, true)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mvc.perform(withIp(post("/api/auth/login")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)));
    }

    private static String tokenFrom(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        return JsonPath.read(result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.token");
    }

    private void expectRegister400(String email, String password, String clan, boolean consent,
                                   String messageFragment) throws Exception {
        String body = mvc.perform(withIp(post("/api/auth/register")).contentType(MediaType.APPLICATION_JSON)
                        .content(json(email, password, clan, consent)))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(body, "$.message")).contains(messageFragment);
    }

    private MockHttpServletRequestBuilder withIp(MockHttpServletRequestBuilder builder) {
        return builder.with(req -> {
            req.setRemoteAddr(ip);
            return req;
        });
    }

    private static String json(String email, String password, String clan, boolean consent) {
        return """
                {"email":"%s","password":"%s","facultyShortCode":%s,"consentGiven":%s}
                """.formatted(email, password, clan == null ? "null" : "\"" + clan + "\"", consent);
    }
}
