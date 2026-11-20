package be.leuven.leuvengo.net;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Google-signed ID tokens for this instance's service account, from the
 * metadata server every Cloud Run instance has. A private Cloud Run service
 * only accepts requests carrying one issued for its own URL.
 */
@Component
public class GoogleIdTokens {

    private static final URI METADATA_URL = URI.create(
            "http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/identity");
    /** Tokens are valid for an hour; renewing early means none expires mid-request. */
    static final Duration REUSE_FOR = Duration.ofMinutes(50);

    private record Token(String value, Instant fetchedAt) {}

    private final URI metadataUrl;
    private final Clock clock;
    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    private final Map<String, Token> cache = new ConcurrentHashMap<>();

    @Autowired
    public GoogleIdTokens(Clock clock) {
        this(METADATA_URL, clock);
    }

    public GoogleIdTokens(URI metadataUrl, Clock clock) {
        this.metadataUrl = metadataUrl;
        this.clock = clock;
    }

    public String forAudience(String audience) throws IOException, InterruptedException {
        Instant now = clock.instant();
        Token cached = cache.get(audience);
        if (cached != null && now.isBefore(cached.fetchedAt().plus(REUSE_FOR))) return cached.value();

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(metadataUrl + "?audience=" + URLEncoder.encode(audience, StandardCharsets.UTF_8)))
                .header("Metadata-Flavor", "Google")
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Metadata server returned HTTP " + response.statusCode());
        }
        Token token = new Token(response.body().strip(), now);
        cache.put(audience, token);
        return token.value();
    }
}
