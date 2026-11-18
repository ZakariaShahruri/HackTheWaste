package be.leuven.leuvengo.whatsapp;

import be.leuven.leuvengo.config.LeuvenGoProperties;
import com.twilio.security.RequestValidator;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Checks the X-Twilio-Signature header, so only Twilio can post fake reports
 * into the webhook. Twilio signs the exact public URL it calls; set
 * TWILIO_WEBHOOK_URL to that URL when running behind a proxy.
 */
@Component
public class TwilioWebhookVerifier {

    private static final Logger log = LoggerFactory.getLogger(TwilioWebhookVerifier.class);

    private final RequestValidator validator;
    private final String webhookUrl;

    public TwilioWebhookVerifier(LeuvenGoProperties props) {
        String token = props.twilio().authToken();
        this.validator = token.isBlank() ? null : new RequestValidator(token);
        this.webhookUrl = props.twilio().webhookUrl();
        if (validator == null) {
            log.warn("TWILIO_AUTH_TOKEN not set: WhatsApp webhook signatures are NOT verified (dev only)");
        }
    }

    public boolean isGenuine(HttpServletRequest request) {
        if (validator == null) return true;

        String signature = request.getHeader(RequestValidator.SIGNATURE_HEADER);
        if (signature == null || signature.isBlank()) return false;

        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((name, values) -> params.put(name, values[0]));
        return validator.validate(signedUrl(request), params, signature);
    }

    private String signedUrl(HttpServletRequest request) {
        if (!webhookUrl.isBlank()) return webhookUrl;
        String query = request.getQueryString();
        return request.getRequestURL() + (query == null ? "" : "?" + query);
    }
}
