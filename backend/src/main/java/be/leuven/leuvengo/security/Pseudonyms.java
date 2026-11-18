package be.leuven.leuvengo.security;

import be.leuven.leuvengo.config.LeuvenGoProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

/**
 * Turns phone numbers into stable identifiers we can store, so the database
 * never holds a number. Keyed with PSEUDONYM_SECRET, so the ids can't be
 * reversed by hashing every Belgian phone number.
 */
@Component
public class Pseudonyms {

    private final SecretKeySpec key;

    public Pseudonyms(LeuvenGoProperties props) {
        this.key = new SecretKeySpec(
                props.security().pseudonymSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /** Internal key for a sender's conversation state (64 hex chars). */
    public String senderKey(String phone) {
        return hmac("sender:" + phone);
    }

    /** Short public id attached to reports, e.g. {@code wa-3f9a1c0b2d4e}. */
    public String publicId(String phone) {
        return "wa-" + hmac("public:" + phone).substring(0, 12);
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is always available", e);
        }
    }
}
