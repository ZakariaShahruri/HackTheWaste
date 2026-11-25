package be.leuven.leuvengo.whatsapp;

import be.leuven.leuvengo.config.LeuvenGoProperties;
import com.twilio.exception.ApiException;
import com.twilio.exception.TwilioException;
import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Sends replies through Twilio's API. Twilio stops waiting for a webhook
 * response after 15 seconds, which a cold start can exceed, so a reply in the
 * response itself would get lost.
 */
@Component
public class TwilioReplies {

    private static final Logger log = LoggerFactory.getLogger(TwilioReplies.class);

    private final TwilioRestClient client;

    @Autowired
    public TwilioReplies(LeuvenGoProperties props) {
        this(props.twilio().hasCredentials()
                ? new TwilioRestClient.Builder(props.twilio().accountSid(), props.twilio().authToken()).build()
                : null);
    }

    TwilioReplies(TwilioRestClient client) {
        this.client = client;
    }

    /** Whether Twilio took the message. If not, the caller has to answer some other way. */
    public boolean send(String from, String to, String text) {
        if (client == null || from == null || to == null) return false;
        try {
            Message.creator(new PhoneNumber(to), new PhoneNumber(from), text).create(client);
            return true;
        } catch (TwilioException e) {
            // Only the code: the message can contain the recipient's number.
            Object reason = e instanceof ApiException api ? api.getCode() : e.getClass().getSimpleName();
            log.warn("Twilio did not accept a WhatsApp reply ({})", reason);
            return false;
        }
    }
}
