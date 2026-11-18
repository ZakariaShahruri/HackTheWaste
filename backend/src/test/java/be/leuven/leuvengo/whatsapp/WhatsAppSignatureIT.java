package be.leuven.leuvengo.whatsapp;

import be.leuven.leuvengo.support.TwilioSignatures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(properties = {
        "leuvengo.twilio.auth-token=it-token",
        "leuvengo.twilio.webhook-url=https://api.leuvengo.example/api/whatsapp/webhook"})
@AutoConfigureMockMvc
class WhatsAppSignatureIT {

    private static final String URL = "https://api.leuvengo.example/api/whatsapp/webhook";

    @Autowired MockMvc mvc;

    @Test
    void unsignedRequestsAreForbidden() throws Exception {
        mvc.perform(post("/api/whatsapp/webhook").param("Body", "hi"))
                .andExpect(status().isForbidden());
    }

    @Test
    void signedRequestsGetAReply() throws Exception {
        String signature = TwilioSignatures.sign("it-token", URL, Map.of("Body", "hi"));
        mvc.perform(post("/api/whatsapp/webhook").param("Body", "hi")
                        .header("X-Twilio-Signature", signature))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<Response>")));
    }
}
