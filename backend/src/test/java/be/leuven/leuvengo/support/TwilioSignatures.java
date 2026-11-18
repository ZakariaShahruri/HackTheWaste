package be.leuven.leuvengo.support;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

/**
 * Signs a webhook the way Twilio documents it: base64(HMAC-SHA1(token,
 * url + each param name and value, sorted by name)). Written independently
 * of Twilio's SDK so tests also check we use the SDK correctly.
 */
public final class TwilioSignatures {

    private TwilioSignatures() {}

    public static String sign(String authToken, String url, Map<String, String> params) {
        StringBuilder data = new StringBuilder(url);
        new TreeMap<>(params).forEach((k, v) -> data.append(k).append(v));
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(authToken.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return Base64.getEncoder().encodeToString(mac.doFinal(data.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
