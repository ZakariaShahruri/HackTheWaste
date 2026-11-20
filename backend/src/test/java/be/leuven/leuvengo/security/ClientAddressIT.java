package be.leuven.leuvengo.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Behind Cloud Run the client address arrives in X-Forwarded-For, where the
 * client also controls every entry except the last one the proxy appends.
 * Runs on a real server because MockMvc bypasses Tomcat's forwarded-header handling.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ClientAddressIT {

    private static final int LOGIN_LIMIT = 10;

    @LocalServerPort
    int port;

    private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

    @Test
    void fakeForwardedAddressesDontResetTheRateLimit() throws Exception {
        for (int i = 1; i <= LOGIN_LIMIT; i++) {
            assertThat(failedLogin("198.51.100." + i + ", 203.0.113.7")).isEqualTo(401);
        }
        assertThat(failedLogin("198.51.100.99, 203.0.113.7")).isEqualTo(429);
    }

    @Test
    void eachRealClientHasItsOwnBudget() throws Exception {
        for (int i = 1; i <= LOGIN_LIMIT; i++) {
            assertThat(failedLogin("203.0.113.20")).isEqualTo(401);
        }
        assertThat(failedLogin("203.0.113.21")).isEqualTo(401);
    }

    private int failedLogin(String forwardedFor) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .header("X-Forwarded-For", forwardedFor)
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"email\":\"nobody@kuleuven.be\",\"password\":\"wrong-password\"}"))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }
}
