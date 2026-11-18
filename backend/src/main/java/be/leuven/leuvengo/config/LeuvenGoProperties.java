package be.leuven.leuvengo.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * All application settings under {@code leuvengo.*}, bound once at startup and
 * validated so a misconfigured deployment fails fast instead of half-working.
 */
@Validated
@ConfigurationProperties("leuvengo")
public record LeuvenGoProperties(
        @Valid @DefaultValue Traffic traffic,
        @Valid @DefaultValue Cors cors,
        @Valid @DefaultValue Gemini gemini,
        @Valid @DefaultValue Twilio twilio,
        @Valid @DefaultValue Storage storage,
        @Valid @DefaultValue Ml ml) {

    /** Hotspot clustering and escalation. */
    public record Traffic(
            @DefaultValue("40") @Positive double clusterRadiusM,
            @DefaultValue("72") @Positive int reportTtlHours,
            @DefaultValue("3") @Positive int workOrderThreshold) {}

    /**
     * Each origin must be a real http(s) URL. This also catches an unset
     * {@code ${ALLOWED_ORIGINS}} placeholder, which the binder would otherwise
     * keep as literal text.
     */
    public record Cors(
            @DefaultValue({"http://localhost:5173", "http://localhost:3000"})
            @NotEmpty
            List<@Pattern(regexp = "\\s*https?://[^\\s${}]+\\s*",
                    message = "must be an http(s) origin such as https://app.example (is ALLOWED_ORIGINS set?)")
                    String> allowedOrigins) {}

    public record Gemini(
            @DefaultValue("") String apiKey,
            @DefaultValue("gemini-2.5-flash-lite") String model,
            @DefaultValue("20s") Duration timeout) {

        public boolean enabled() {
            return !apiKey.isBlank();
        }
    }

    public record Twilio(
            @DefaultValue("") String accountSid,
            @DefaultValue("") String authToken) {

        public boolean hasCredentials() {
            return !accountSid.isBlank() && !authToken.isBlank();
        }
    }

    public record Storage(
            @DefaultValue("") String azureConnectionString,
            @DefaultValue("trash-photos") String azureContainer) {}

    public record Ml(@DefaultValue("http://localhost:8000") String apiUrl) {}
}
