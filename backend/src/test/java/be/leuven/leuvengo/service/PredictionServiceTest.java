package be.leuven.leuvengo.service;

import be.leuven.leuvengo.net.GoogleIdTokens;
import be.leuven.leuvengo.support.TestProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/** Talks to a fake ML service over plain http, like the real deployment does. */
class PredictionServiceTest {

    private HttpServer server;
    private final AtomicReference<String> upgradeHeader = new AtomicReference<>();
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private PredictionService predictions;
    private String url;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/predict/grid", exchange -> {
            upgradeHeader.set(exchange.getRequestHeaders().getFirst("Upgrade"));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = """
                    {"time": "2026-11-18T12:00:00", "count": 1, "model": "rf",
                     "points": [{"lat": 50.87, "lng": 4.7, "likelihood": 0.42, "extra": 1}]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.createContext("/identity", exchange -> {
            byte[] body = "id-token-for-ml".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        url = "http://localhost:" + server.getAddress().getPort();
        predictions = service(Map.of("ml.api-url", url));
    }

    private PredictionService service(Map<String, String> properties) {
        GoogleIdTokens idTokens = new GoogleIdTokens(URI.create(url + "/identity"), Clock.systemUTC());
        return new PredictionService(TestProperties.with(properties), idTokens, JsonMapper.builder().build());
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void sendsAPlainHttp11RequestAndParsesTheGrid() {
        var points = predictions.predictGrid(LocalDateTime.of(2026, 11, 18, 12, 0), 5).orElseThrow();

        assertThat(upgradeHeader.get()).as("uvicorn drops the body of h2c upgrade requests").isNull();
        assertThat(requestBody.get()).contains("\"lat_steps\":5", "\"time\":\"2026-11-18T12:00:00\"");
        assertThat(points).containsExactly(new PredictionService.PredictPoint(50.87, 4.7, 0.42));
    }

    @Test
    void sendsNoCredentialsByDefault() {
        predictions.predictGrid(LocalDateTime.of(2026, 11, 18, 12, 0), 5).orElseThrow();

        assertThat(authorization.get()).isNull();
    }

    @Test
    void provesItsIdentityToAPrivateCloudRunService() {
        predictions = service(Map.of("ml.api-url", url, "ml.google-id-token", "true"));

        predictions.predictGrid(LocalDateTime.of(2026, 11, 18, 12, 0), 5).orElseThrow();

        assertThat(authorization.get()).isEqualTo("Bearer id-token-for-ml");
    }

    @Test
    void anUnreachableServiceMeansNoPrediction() {
        server.stop(0);
        assertThat(predictions.predictGrid(LocalDateTime.now(), 5)).isEmpty();
    }
}
