package be.leuven.leuvengo.whatsapp;

import be.leuven.leuvengo.config.LeuvenGoProperties;
import be.leuven.leuvengo.net.SafeHttpFetcher;
import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;

/** Downloads photos users send over WhatsApp. Only api.twilio.com ever sees our credentials. */
@Component
public class TwilioMediaFetcher {

    /** Formats {@link be.leuven.leuvengo.photo.ImageNormalizer} can decode. WhatsApp sends photos as JPEG. */
    static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png");
    static final long MAX_BYTES = 8L * 1024 * 1024;

    private final SafeHttpFetcher fetcher;
    private final SafeHttpFetcher.Policy policy;

    public TwilioMediaFetcher(LeuvenGoProperties props) {
        this.fetcher = new SafeHttpFetcher(HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(5))
                .build());
        LeuvenGoProperties.Twilio twilio = props.twilio();
        String authorization = twilio.hasCredentials()
                ? "Basic " + Base64.getEncoder().encodeToString(
                        (twilio.accountSid() + ":" + twilio.authToken()).getBytes(StandardCharsets.UTF_8))
                : null;
        this.policy = new SafeHttpFetcher.Policy(
                Set.of("https"), Set.of("api.twilio.com"), authorization,
                IMAGE_TYPES, MAX_BYTES, 3, Duration.ofSeconds(15));
    }

    public SafeHttpFetcher.Download fetch(String mediaUrl) throws SafeHttpFetcher.FetchException {
        return fetcher.fetch(mediaUrl, policy);
    }
}
