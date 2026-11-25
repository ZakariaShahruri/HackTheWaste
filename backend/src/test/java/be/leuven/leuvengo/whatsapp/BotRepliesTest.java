package be.leuven.leuvengo.whatsapp;

import be.leuven.leuvengo.service.GeminiService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class BotRepliesTest {

    @Test
    void withoutASiteUrlThereIsNoLink() {
        assertThat(BotReplies.reported(GeminiService.ScoreResult.unavailable(), Optional.empty(), ""))
                .endsWith("Thanks for keeping Leuven clean! 🌿");
    }
}
