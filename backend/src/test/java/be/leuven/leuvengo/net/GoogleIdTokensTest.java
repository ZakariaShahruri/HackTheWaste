package be.leuven.leuvengo.net;

import be.leuven.leuvengo.support.MutableClock;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleIdTokensTest {

    private static final String AUDIENCE = "https://ml-abc.a.run.app";

    private HttpServer metadata;
    private final List<String> queries = new CopyOnWriteArrayList<>();
    private final AtomicInteger issued = new AtomicInteger();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-20T12:00:00Z"));
    private GoogleIdTokens tokens;

    @BeforeEach
    void start() throws IOException {
        metadata = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        metadata.createContext("/identity", exchange -> {
            boolean fromInstance = "Google".equals(exchange.getRequestHeaders().getFirst("Metadata-Flavor"));
            queries.add(exchange.getRequestURI().getQuery());
            byte[] body = ("token-" + issued.incrementAndGet() + "\n").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(fromInstance ? 200 : 403, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        metadata.start();
        tokens = new GoogleIdTokens(URI.create("http://localhost:" + metadata.getAddress().getPort() + "/identity"), clock);
    }

    @AfterEach
    void stop() {
        metadata.stop(0);
    }

    @Test
    void asksTheMetadataServerForATokenForThatAudience() throws Exception {
        assertThat(tokens.forAudience(AUDIENCE)).isEqualTo("token-1");
        assertThat(queries).containsExactly("audience=" + AUDIENCE);
    }

    @Test
    void reusesATokenUntilItIsCloseToExpiring() throws Exception {
        tokens.forAudience(AUDIENCE);
        clock.advance(GoogleIdTokens.REUSE_FOR.minus(Duration.ofSeconds(1)));
        assertThat(tokens.forAudience(AUDIENCE)).isEqualTo("token-1");

        clock.advance(Duration.ofSeconds(1));
        assertThat(tokens.forAudience(AUDIENCE)).isEqualTo("token-2");
    }

    @Test
    void anErrorFromTheMetadataServerIsNotUsedAsAToken() {
        tokens = new GoogleIdTokens(URI.create("http://localhost:" + metadata.getAddress().getPort() + "/missing"), clock);

        assertThatThrownBy(() -> tokens.forAudience(AUDIENCE)).hasMessageContaining("HTTP 404");
    }
}
