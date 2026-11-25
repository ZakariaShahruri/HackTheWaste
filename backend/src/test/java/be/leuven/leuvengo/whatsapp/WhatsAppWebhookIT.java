package be.leuven.leuvengo.whatsapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WhatsAppWebhookIT {

    private static final String BOT = "whatsapp:+14155238886";
    private static final String ANA = "whatsapp:+32470000001";

    @Autowired MockMvc mvc;
    @MockitoBean WhatsAppBot bot;
    @MockitoBean TwilioReplies replies;

    @BeforeEach
    void setUp() {
        when(bot.handle(any())).thenReturn("the reply");
    }

    @Test
    void repliesThroughTheApiSoASlowResponseLosesNothing() throws Exception {
        when(replies.send(BOT, ANA, "the reply")).thenReturn(true);

        message().andExpect(content().string(not(containsString("the reply"))));
        verify(replies).send(BOT, ANA, "the reply");
    }

    @Test
    void repliesInTheResponseWhenTheApiCannotBeUsed() throws Exception {
        when(replies.send(anyString(), anyString(), anyString())).thenReturn(false);

        message().andExpect(content().string(containsString("the reply")));
    }

    @Test
    void deliveryReceiptsAreNotMessages() throws Exception {
        for (String receipt : new String[] {"sent", "delivered", "read", "failed"}) {
            mvc.perform(post("/api/whatsapp/webhook")
                            .param("From", BOT).param("To", ANA).param("MessageStatus", receipt))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("<Message>"))));
        }
        verify(bot, never()).handle(any());
        verify(replies, never()).send(any(), any(), any());
    }

    private ResultActions message() throws Exception {
        return mvc.perform(post("/api/whatsapp/webhook")
                        .param("From", ANA).param("To", BOT).param("Body", "hi"))
                .andExpect(status().isOk());
    }
}
