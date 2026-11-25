package be.leuven.leuvengo.whatsapp;

import com.twilio.http.HttpClient;
import com.twilio.http.Request;
import com.twilio.http.Response;
import com.twilio.http.TwilioRestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class TwilioRepliesTest {

    private static final String BOT = "whatsapp:+14155238886";
    private static final String ANA = "whatsapp:+32470000001";

    private Request sent;

    @Test
    void sendsTheReplyFromTheBotToTheSender() {
        assertThat(twilioAnswering("{\"sid\":\"SM1\"}", 201).send(BOT, ANA, "Hi Ana")).isTrue();

        assertThat(sent.getUrl()).isEqualTo("https://api.twilio.com/2010-04-01/Accounts/AC1/Messages.json");
        assertThat(sent.getPostParams())
                .containsEntry("From", List.of(BOT))
                .containsEntry("To", List.of(ANA))
                .containsEntry("Body", List.of("Hi Ana"));
    }

    @Test
    void aRefusalIsReportedWithoutLoggingTheNumber(CapturedOutput output) {
        TwilioReplies replies = twilioAnswering(
                "{\"code\":21211,\"message\":\"Invalid 'To' number " + ANA + "\",\"status\":400}", 400);

        assertThat(replies.send(BOT, ANA, "Hi Ana")).isFalse();
        assertThat(output).contains("21211").doesNotContain("32470000001");
    }

    @Test
    void withoutCredentialsNothingIsSent() {
        assertThat(new TwilioReplies((TwilioRestClient) null).send(BOT, ANA, "Hi Ana")).isFalse();
    }

    private TwilioReplies twilioAnswering(String json, int status) {
        HttpClient http = new HttpClient() {
            @Override
            public Response makeRequest(Request request) {
                sent = request;
                return new Response(json, status);
            }
        };
        return new TwilioReplies(new TwilioRestClient.Builder("AC1", "token").httpClient(http).build());
    }
}
