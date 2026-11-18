package be.leuven.leuvengo.service;

import be.leuven.leuvengo.config.LeuvenGoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Asks Gemini whether a photo is genuine and how clean the place in it is. */
@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";
    private static final int MAX_TAGS = 3;

    private static final String PROMPT =
            "You are a fraud-detecting cleanliness inspector for a city reporting app. "
            + "First, decide if this photo is FAKE. Mark it fake if ANY of these apply: "
            + "(1) the image appears to be a photo of a screen, monitor, TV, or phone display "
            + "(look for screen glare, pixel grids, Moiré patterns, device bezels, or curved display edges); "
            + "(2) the image looks like a stock photo, watermarked image, or search-engine result; "
            + "(3) the image shows an indoor scene with no outdoor urban context; "
            + "(4) there is no real-world depth, lighting, or perspective — it looks flat or digitally generated. "
            + "If the photo is NOT fake, rate the cleanliness of the outdoor urban space on a scale of 0 to 100 "
            + "where 100 = perfectly clean and 0 = completely covered in hazardous waste. "
            + "Respond with ONLY valid JSON: "
            + "{\"fake\": <true|false>, \"reason\": \"<one short sentence why it is fake, or null if genuine>\", "
            + "\"score\": <0-100 or null if fake>, \"description\": \"<one short sentence or null if fake>\", "
            + "\"tags\": [\"<tag1>\", \"<tag2>\"]}. "
            + "Tags must name visible waste types such as plastic, glass, paper, food_waste, cigarettes. "
            + "Include at most 3 tags. If fake, tags may be empty.";

    /**
     * The verdict on one photo. When {@code aiAvailable} is false the AI could
     * not be asked (no key, outage) and {@code score} is a neutral placeholder.
     */
    public record ScoreResult(boolean aiAvailable, boolean fake, String fakeReason,
                              int score, String description, List<String> tags) {

        static final int NEUTRAL_SCORE = 50;

        public static ScoreResult unavailable() {
            return new ScoreResult(false, false, null, NEUTRAL_SCORE, null, List.of());
        }

        public static ScoreResult genuine(int score, String description, List<String> tags) {
            return new ScoreResult(true, false, null, score, description, tags);
        }

        public static ScoreResult rejected(String reason) {
            return new ScoreResult(true, true, reason, 0, null, List.of());
        }
    }

    private final LeuvenGoProperties.Gemini config;
    private final JsonMapper json;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public GeminiService(LeuvenGoProperties props, JsonMapper json) {
        this.config = props.gemini();
        this.json = json;
    }

    /** Never throws: any failure degrades to {@link ScoreResult#unavailable()}. */
    public ScoreResult scorePhoto(byte[] image, String mimeType) {
        if (!config.enabled()) {
            log.warn("GEMINI_API_KEY not configured, photo gets a neutral score");
            return ScoreResult.unavailable();
        }
        try {
            ScoreResult result = parse(call(image, mimeType));
            log.info("Gemini verdict: fake={} score={} tags={}", result.fake(), result.score(), result.tags());
            return result;
        } catch (IOException | JacksonException e) {
            log.error("Gemini scoring failed: {}", e.getMessage());
            return ScoreResult.unavailable();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ScoreResult.unavailable();
        }
    }

    private String call(byte[] image, String mimeType) throws IOException, InterruptedException {
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(
                        Map.of("text", PROMPT),
                        Map.of("inlineData", Map.of(
                                "mimeType", mimeType,
                                "data", Base64.getEncoder().encodeToString(image)))))),
                "generationConfig", Map.of("responseMimeType", "application/json"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT.formatted(config.model())))
                .timeout(config.timeout())
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", config.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Gemini API returned HTTP " + response.statusCode());
        }
        return response.body();
    }

    /** Turns a generateContent response into a verdict, distrusting everything in it. */
    ScoreResult parse(String responseBody) {
        String text = json.readTree(responseBody).at("/candidates/0/content/parts/0/text").asString("").strip();
        if (text.startsWith("```")) {
            text = text.replaceAll("(?s)```[a-z]*\\n?", "").strip();
        }
        if (text.isEmpty()) {
            return ScoreResult.unavailable();
        }

        JsonNode verdict = json.readTree(text);
        if (verdict.path("fake").asBoolean(false)) {
            return ScoreResult.rejected(optionalText(verdict, "reason"));
        }
        JsonNode score = verdict.path("score");
        if (!score.isNumber()) {
            return ScoreResult.unavailable();
        }
        return ScoreResult.genuine(
                Math.clamp(Math.round(score.asDouble()), 0, 100),
                optionalText(verdict, "description"),
                tags(verdict.path("tags")));
    }

    private static String optionalText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isString()) return null;
        String text = value.asString().strip();
        return text.isEmpty() ? null : truncate(text, 200);
    }

    /** Lower-case snake_case words only, at most three. */
    private static List<String> tags(JsonNode node) {
        List<String> tags = new ArrayList<>();
        if (!node.isArray()) return tags;
        for (JsonNode tag : node) {
            if (tags.size() == MAX_TAGS) break;
            String clean = tag.asString("").toLowerCase(Locale.ROOT).strip().replace(' ', '_');
            if (clean.matches("[a-z_]{2,24}") && !tags.contains(clean)) {
                tags.add(clean);
            }
        }
        return List.copyOf(tags);
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
