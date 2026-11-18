package be.leuven.leuvengo.service;

import be.leuven.leuvengo.support.TestProperties;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiServiceTest {

    private final JsonMapper json = JsonMapper.builder().build();
    private final GeminiService gemini = new GeminiService(TestProperties.defaults(), json);

    @Test
    void parsesAGenuineVerdict() {
        GeminiService.ScoreResult r = gemini.parse(response("""
                {"fake": false, "reason": null, "score": 23, "description": "Overflowing bin",
                 "tags": ["plastic", "Food Waste", "plastic", "glass", "paper"]}"""));

        assertThat(r.aiAvailable()).isTrue();
        assertThat(r.fake()).isFalse();
        assertThat(r.score()).isEqualTo(23);
        assertThat(r.description()).isEqualTo("Overflowing bin");
        assertThat(r.tags()).containsExactly("plastic", "food_waste", "glass");
    }

    @Test
    void parsesAFakeVerdict() {
        GeminiService.ScoreResult r = gemini.parse(response(
                "{\"fake\": true, \"reason\": \"Photo of a monitor\", \"score\": null, \"tags\": []}"));

        assertThat(r.fake()).isTrue();
        assertThat(r.fakeReason()).isEqualTo("Photo of a monitor");
    }

    @Test
    void clampsScoresAndDropsJunkTags() {
        GeminiService.ScoreResult r = gemini.parse(response(
                "{\"fake\": false, \"score\": 140, \"tags\": [\"<script>\", 7, \"cigarettes\"]}"));

        assertThat(r.score()).isEqualTo(100);
        assertThat(r.tags()).containsExactly("cigarettes");
    }

    @Test
    void toleratesCodeFences() {
        GeminiService.ScoreResult r = gemini.parse(response("```json\\n{\\\"fake\\\": false, \\\"score\\\": 60}\\n```", true));
        assertThat(r.score()).isEqualTo(60);
    }

    @Test
    void aMissingScoreMeansTheAiDidNotReallyAnswer() {
        assertThat(gemini.parse(response("{\"fake\": false}")).aiAvailable()).isFalse();
        assertThat(gemini.parse("{\"candidates\": []}").aiAvailable()).isFalse();
    }

    @Test
    void withoutAnApiKeyEveryPhotoGetsANeutralUnscoredResult() {
        GeminiService.ScoreResult r = gemini.scorePhoto(new byte[]{1}, "image/jpeg");
        assertThat(r.aiAvailable()).isFalse();
        assertThat(r.score()).isEqualTo(50);
    }

    @Test
    void usesTheConfiguredModel() {
        assertThat(TestProperties.with(Map.of("gemini.model", "gemini-3-flash")).gemini().model())
                .isEqualTo("gemini-3-flash");
    }

    /** Wraps the model's text answer the way the generateContent API does. */
    private String response(String modelText) {
        return json.writeValueAsString(Map.of("candidates", java.util.List.of(
                Map.of("content", Map.of("parts", java.util.List.of(Map.of("text", modelText)))))));
    }

    private static String response(String alreadyEscapedText, boolean raw) {
        return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"" + alreadyEscapedText + "\"}]}}]}";
    }
}
