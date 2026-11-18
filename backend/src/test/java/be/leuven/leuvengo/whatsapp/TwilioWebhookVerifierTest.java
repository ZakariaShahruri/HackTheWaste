package be.leuven.leuvengo.whatsapp;

import be.leuven.leuvengo.support.TestProperties;
import be.leuven.leuvengo.support.TwilioSignatures;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TwilioWebhookVerifierTest {

    private static final String TOKEN = "test-auth-token";
    private static final String PUBLIC_URL = "https://api.leuvengo.example/api/whatsapp/webhook";
    private static final Map<String, String> PARAMS =
            Map.of("From", "whatsapp:+32470000000", "Latitude", "50.8525", "Longitude", "4.672");

    @Test
    void acceptsAGenuineSignatureOverTheConfiguredUrl() {
        TwilioWebhookVerifier verifier = verifier(PUBLIC_URL);
        assertThat(verifier.isGenuine(request(TwilioSignatures.sign(TOKEN, PUBLIC_URL, PARAMS)))).isTrue();
    }

    @Test
    void rejectsMissingOrWrongSignatures() {
        TwilioWebhookVerifier verifier = verifier(PUBLIC_URL);
        assertThat(verifier.isGenuine(request(null))).isFalse();
        assertThat(verifier.isGenuine(request("bm9wZQ=="))).isFalse();
        assertThat(verifier.isGenuine(request(TwilioSignatures.sign("other-token", PUBLIC_URL, PARAMS)))).isFalse();
    }

    @Test
    void rejectsTamperedParameters() {
        String signature = TwilioSignatures.sign(TOKEN, PUBLIC_URL, PARAMS);
        MockHttpServletRequest tampered = request(signature);
        tampered.setParameter("Latitude", "50.0");
        assertThat(verifier(PUBLIC_URL).isGenuine(tampered)).isFalse();
    }

    @Test
    void fallsBackToTheRequestUrlWhenNoneIsConfigured() {
        String requestUrl = "http://localhost/api/whatsapp/webhook";
        assertThat(verifier("").isGenuine(request(TwilioSignatures.sign(TOKEN, requestUrl, PARAMS)))).isTrue();
    }

    @Test
    void acceptsEverythingWhenNoTokenIsConfigured() {
        TwilioWebhookVerifier devVerifier = new TwilioWebhookVerifier(TestProperties.defaults());
        assertThat(devVerifier.isGenuine(request(null))).isTrue();
    }

    private static TwilioWebhookVerifier verifier(String webhookUrl) {
        return new TwilioWebhookVerifier(TestProperties.with(Map.of(
                "twilio.auth-token", TOKEN, "twilio.webhook-url", webhookUrl)));
    }

    private static MockHttpServletRequest request(String signature) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/whatsapp/webhook");
        PARAMS.forEach(request::setParameter);
        if (signature != null) request.addHeader("X-Twilio-Signature", signature);
        return request;
    }
}
