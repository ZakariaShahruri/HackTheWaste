package be.leuven.leuvengo.web;

import be.leuven.leuvengo.whatsapp.TwilioReplies;
import be.leuven.leuvengo.whatsapp.TwilioWebhookVerifier;
import be.leuven.leuvengo.whatsapp.WhatsAppBot;
import com.twilio.twiml.MessagingResponse;
import com.twilio.twiml.messaging.Message;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.BindParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Twilio's WhatsApp webhook: checks the signature, then passes the bot's answer back. */
@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppController {

    /** The fields of Twilio's request we use. Any of them may be missing. */
    record TwilioRequest(
            @BindParam("From") String from,
            @BindParam("To") String to,
            @BindParam("Body") String body,
            @BindParam("Latitude") Double lat,
            @BindParam("Longitude") Double lng,
            @BindParam("MediaUrl0") String mediaUrl,
            @BindParam("MediaContentType0") String mediaType,
            @BindParam("ProfileName") String profileName,
            @BindParam("MessageStatus") String messageStatus) {

        /** An update about a message we sent ("sent", "delivered", "read"), not a message to us. */
        boolean isDeliveryReceipt() {
            return messageStatus != null && !messageStatus.equals("received");
        }

        WhatsAppBot.Inbound toInbound() {
            return new WhatsAppBot.Inbound(from, profileName, body, lat, lng, mediaUrl, mediaType);
        }
    }

    private final TwilioWebhookVerifier verifier;
    private final WhatsAppBot bot;
    private final TwilioReplies replies;

    public WhatsAppController(TwilioWebhookVerifier verifier, WhatsAppBot bot, TwilioReplies replies) {
        this.verifier = verifier;
        this.bot = bot;
        this.replies = replies;
    }

    @PostMapping(value = "/webhook", produces = MediaType.APPLICATION_XML_VALUE)
    public String webhook(HttpServletRequest request, @ModelAttribute TwilioRequest message) {
        if (!verifier.isGenuine(request)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid Twilio signature");
        }
        MessagingResponse.Builder response = new MessagingResponse.Builder();
        if (!message.isDeliveryReceipt()) {
            String reply = bot.handle(message.toInbound());
            if (!replies.send(message.to(), message.from(), reply)) {
                response.message(new Message.Builder(reply).build());
            }
        }
        return response.build().toXml();
    }
}
