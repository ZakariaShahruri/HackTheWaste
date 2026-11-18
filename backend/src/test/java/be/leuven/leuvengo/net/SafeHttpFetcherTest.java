package be.leuven.leuvengo.net;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs against a real local server. "localhost" plays the trusted host and
 * "127.0.0.1" plays a foreign host (e.g. a CDN behind a redirect).
 */
class SafeHttpFetcherTest {

    private static final String AUTH = "Basic c2VjcmV0";

    private HttpServer server;
    private int port;
    /** Authorization header seen per request path ("-" when absent). */
    private final Map<String, String> authSeen = new ConcurrentHashMap<>();
    private final List<String> hits = new CopyOnWriteArrayList<>();

    private final SafeHttpFetcher fetcher = new SafeHttpFetcher(
            HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build());

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        port = server.getAddress().getPort();
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            hits.add(path);
            authSeen.put(path, String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")).replace("null", "-"));
            switch (path) {
                case "/photo.jpg" -> respond(exchange, 200, "image/jpeg", new byte[100]);
                case "/page.html" -> respond(exchange, 200, "text/html", "<html>".getBytes());
                case "/huge.jpg" -> respond(exchange, 200, "image/jpeg", new byte[2_000]);
                case "/to-cdn" -> redirect(exchange, "http://127.0.0.1:" + port + "/photo.jpg");
                case "/to-ftp" -> redirect(exchange, "ftp://127.0.0.1/photo.jpg");
                case "/loop" -> redirect(exchange, "/loop");
                default -> respond(exchange, 404, "text/plain", new byte[0]);
            }
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void downloadsFromTheTrustedHostWithCredentials() throws Exception {
        SafeHttpFetcher.Download d = fetcher.fetch(local("/photo.jpg"), policy());

        assertThat(d.bytes()).hasSize(100);
        assertThat(d.contentType()).isEqualTo("image/jpeg");
        assertThat(authSeen.get("/photo.jpg")).isEqualTo(AUTH);
    }

    @Test
    void followsRedirectsButNeverForwardsCredentials() throws Exception {
        fetcher.fetch(local("/to-cdn"), policy());

        assertThat(authSeen.get("/to-cdn")).isEqualTo(AUTH);
        assertThat(authSeen.get("/photo.jpg")).isEqualTo("-");
    }

    @Test
    void refusesUntrustedHostsWithoutConnecting() {
        for (String url : List.of(
                "http://127.0.0.1:" + port + "/photo.jpg",
                "http://localhost.evil.example:" + port + "/photo.jpg",
                "http://localhost@127.0.0.1:" + port + "/photo.jpg",
                "http://127.0.0.1:" + port + "/photo.jpg?localhost",
                "/photo.jpg",
                "not a url")) {
            assertThatThrownBy(() -> fetcher.fetch(url, policy()))
                    .as(url).isInstanceOf(SafeHttpFetcher.FetchException.class);
        }
        assertThat(hits).isEmpty();
    }

    @Test
    void refusesOtherSchemes() {
        assertThatThrownBy(() -> fetcher.fetch("https://localhost:" + port + "/photo.jpg", policy()))
                .hasMessageContaining("Scheme");
        assertThatThrownBy(() -> fetcher.fetch(local("/to-ftp"), policy()))
                .hasMessageContaining("Scheme");
    }

    @Test
    void refusesNonImages() {
        assertThatThrownBy(() -> fetcher.fetch(local("/page.html"), policy()))
                .hasMessageContaining("Content type not allowed: text/html");
    }

    @Test
    void refusesFilesOverTheCap() {
        assertThatThrownBy(() -> fetcher.fetch(local("/huge.jpg"), policy()))
                .hasMessageContaining("too large");
    }

    @Test
    void givesUpOnRedirectLoops() {
        assertThatThrownBy(() -> fetcher.fetch(local("/loop"), policy()))
                .hasMessageContaining("Too many redirects");
        assertThat(hits).hasSize(4);
    }

    @Test
    void reportsHttpErrors() {
        assertThatThrownBy(() -> fetcher.fetch(local("/missing"), policy()))
                .hasMessage("HTTP 404");
    }

    @Test
    void rejectsClientsThatFollowRedirectsThemselves() {
        assertThatThrownBy(() -> new SafeHttpFetcher(
                HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private String local(String path) {
        return "http://localhost:" + port + path;
    }

    private static SafeHttpFetcher.Policy policy() {
        return new SafeHttpFetcher.Policy(Set.of("http"), Set.of("localhost"), AUTH,
                Set.of("image/jpeg"), 1_000, 3, Duration.ofSeconds(5));
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status,
                                String type, byte[] body) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", type);
        exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static void redirect(com.sun.net.httpserver.HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().add("Location", location);
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }
}
