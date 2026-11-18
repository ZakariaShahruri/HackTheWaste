package be.leuven.leuvengo.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;

/**
 * Downloads a file from a URL we don't control (e.g. one sent to a webhook)
 * without becoming an SSRF hole or leaking credentials:
 * <ul>
 *   <li>the first URL must use an allowed scheme and an exactly-matching trusted host;</li>
 *   <li>redirects are followed by hand, and credentials only go to trusted hosts;</li>
 *   <li>the response must have an allowed content type and stay under a size cap.</li>
 * </ul>
 */
public final class SafeHttpFetcher {

    /** What a download is allowed to do. {@code authorization} may be null. */
    public record Policy(Set<String> schemes,
                         Set<String> trustedHosts,
                         String authorization,
                         Set<String> contentTypes,
                         long maxBytes,
                         int maxRedirects,
                         Duration timeout) {}

    public record Download(byte[] bytes, String contentType) {}

    /** Any reason we refused or failed to fetch; the message is safe to log. */
    public static class FetchException extends IOException {
        public FetchException(String message) {
            super(message);
        }
    }

    private final HttpClient http;

    /** The client must not follow redirects itself. */
    public SafeHttpFetcher(HttpClient http) {
        if (http.followRedirects() != HttpClient.Redirect.NEVER) {
            throw new IllegalArgumentException("SafeHttpFetcher needs a client with Redirect.NEVER");
        }
        this.http = http;
    }

    public Download fetch(String url, Policy policy) throws FetchException {
        URI uri = parse(url);
        requireScheme(uri, policy);
        if (!isTrusted(uri, policy)) {
            throw new FetchException("Host not allowed: " + uri.getHost());
        }

        for (int hop = 0; hop <= policy.maxRedirects(); hop++) {
            HttpResponse<InputStream> response = send(uri, policy);
            int status = response.statusCode();

            if (status >= 300 && status < 400) {
                drain(response);
                String location = response.headers().firstValue("Location")
                        .orElseThrow(() -> new FetchException("Redirect without Location"));
                uri = uri.resolve(location);
                requireScheme(uri, policy);
                continue;
            }
            if (status != 200) {
                drain(response);
                throw new FetchException("HTTP " + status);
            }
            return read(response, policy);
        }
        throw new FetchException("Too many redirects");
    }

    private HttpResponse<InputStream> send(URI uri, Policy policy) throws FetchException {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri).timeout(policy.timeout()).GET();
        if (policy.authorization() != null && isTrusted(uri, policy)) {
            request.header("Authorization", policy.authorization());
        }
        try {
            return http.send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException e) {
            throw new FetchException("Request failed: " + e.getClass().getSimpleName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FetchException("Interrupted");
        }
    }

    private static Download read(HttpResponse<InputStream> response, Policy policy) throws FetchException {
        String contentType = response.headers().firstValue("Content-Type")
                .map(v -> v.split(";")[0].trim().toLowerCase(Locale.ROOT))
                .orElse("");
        if (!policy.contentTypes().contains(contentType)) {
            drain(response);
            throw new FetchException("Content type not allowed: " + (contentType.isEmpty() ? "none" : contentType));
        }
        long declared = response.headers().firstValueAsLong("Content-Length").orElse(-1);
        if (declared > policy.maxBytes()) {
            drain(response);
            throw new FetchException("File too large");
        }
        try (InputStream body = response.body()) {
            byte[] bytes = body.readNBytes((int) Math.min(Integer.MAX_VALUE - 8, policy.maxBytes() + 1));
            if (bytes.length > policy.maxBytes()) {
                throw new FetchException("File too large");
            }
            return new Download(bytes, contentType);
        } catch (FetchException e) {
            throw e;
        } catch (IOException e) {
            throw new FetchException("Read failed: " + e.getClass().getSimpleName());
        }
    }

    private static URI parse(String url) throws FetchException {
        try {
            URI uri = new URI(url);
            if (!uri.isAbsolute() || uri.getHost() == null) {
                throw new FetchException("Not an absolute URL");
            }
            return uri;
        } catch (java.net.URISyntaxException e) {
            throw new FetchException("Malformed URL");
        }
    }

    private static void requireScheme(URI uri, Policy policy) throws FetchException {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!policy.schemes().contains(scheme)) {
            throw new FetchException("Scheme not allowed: " + scheme);
        }
    }

    /** Exact host match, and no "user@host" tricks. */
    private static boolean isTrusted(URI uri, Policy policy) {
        return uri.getRawUserInfo() == null
                && uri.getHost() != null
                && policy.trustedHosts().contains(uri.getHost().toLowerCase(Locale.ROOT));
    }

    private static void drain(HttpResponse<InputStream> response) {
        try (InputStream ignored = response.body()) {
            // closing releases the connection
        } catch (IOException ignored) {
            // nothing useful to do
        }
    }
}
