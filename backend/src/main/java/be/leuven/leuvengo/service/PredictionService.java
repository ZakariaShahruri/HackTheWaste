package be.leuven.leuvengo.service;

import be.leuven.leuvengo.config.LeuvenGoProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Asks the ML service how likely litter is across Leuven at a given time. */
@Service
public class PredictionService {

    private static final Logger log = LoggerFactory.getLogger(PredictionService.class);
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /** The area the model was trained on. */
    private static final GeoBox GRID_AREA = new GeoBox(50.855, 50.905, 4.670, 4.730);

    private final String mlApiUrl;
    private final JsonMapper mapper;
    /**
     * HTTP/1.1 on purpose: over plain http the JDK client otherwise tries an
     * h2c upgrade, which uvicorn rejects, dropping the request body (422).
     */
    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    public PredictionService(LeuvenGoProperties props, JsonMapper mapper) {
        this.mlApiUrl = props.ml().apiUrl();
        this.mapper = mapper;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PredictPoint(double lat, double lng, double likelihood) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GridResponse(List<PredictPoint> points) {}

    /**
     * A {@code steps} x {@code steps} grid of likelihoods for Leuven local time {@code at},
     * or empty when the ML service can't be reached.
     */
    public Optional<List<PredictPoint>> predictGrid(LocalDateTime at, int steps) {
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "time", at.format(ISO),
                    "lat_min", GRID_AREA.latMin(),
                    "lat_max", GRID_AREA.latMax(),
                    "lng_min", GRID_AREA.lngMin(),
                    "lng_max", GRID_AREA.lngMax(),
                    "lat_steps", steps,
                    "lng_steps", steps));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(mlApiUrl + "/predict/grid"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("ML service returned HTTP {}", response.statusCode());
                return Optional.empty();
            }
            return Optional.of(mapper.readValue(response.body(), GridResponse.class).points());
        } catch (IOException | JacksonException e) {
            log.warn("ML service unavailable: {}", e.getMessage());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }
}
