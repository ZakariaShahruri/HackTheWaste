package be.leuven.leuvengo.web;

import be.leuven.leuvengo.whatsapp.TwilioWebhookVerifier;
import be.leuven.leuvengo.whatsapp.WhatsAppBot;
import com.twilio.twiml.MessagingResponse;
import com.twilio.twiml.messaging.Message;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Twilio's WhatsApp webhook: checks the signature, then lets the bot answer in TwiML. */
@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppController {

    private final TwilioWebhookVerifier verifier;
    private final WhatsAppBot bot;

    public WhatsAppController(TwilioWebhookVerifier verifier, WhatsAppBot bot) {
        this.verifier = verifier;
        this.bot = bot;
    }

    @PostMapping(value = "/webhook", produces = MediaType.APPLICATION_XML_VALUE)
    public String webhook(
            HttpServletRequest request,
            @RequestParam(name = "From", required = false) String from,
            @RequestParam(name = "Body", required = false) String body,
            @RequestParam(name = "Latitude", required = false) Double lat,
            @RequestParam(name = "Longitude", required = false) Double lng,
            @RequestParam(name = "MediaUrl0", required = false) String mediaUrl) {

        if (!verifier.isGenuine(request)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid Twilio signature");
        }
        String reply = bot.handle(new WhatsAppBot.Inbound(from, body, lat, lng, mediaUrl));
        return new MessagingResponse.Builder()
                .message(new Message.Builder(reply).build())
                .build()
                .toXml();
    }
}
