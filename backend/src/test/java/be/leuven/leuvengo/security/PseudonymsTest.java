package be.leuven.leuvengo.security;

import be.leuven.leuvengo.support.TestProperties;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PseudonymsTest {

    private static final String PHONE = "whatsapp:+32470000001";

    private final Pseudonyms pseudonyms = pseudonyms("first-secret-0123456789");

    @Test
    void idsAreStableAndDontContainTheNumber() {
        assertThat(pseudonyms.senderKey(PHONE)).isEqualTo(pseudonyms.senderKey(PHONE)).hasSize(64);
        assertThat(pseudonyms.publicId(PHONE)).isEqualTo(pseudonyms.publicId(PHONE)).matches("wa-[0-9a-f]{12}");
        assertThat(pseudonyms.senderKey(PHONE)).doesNotContain("32470000001");
    }

    @Test
    void differentNumbersGetDifferentIds() {
        assertThat(pseudonyms.publicId(PHONE)).isNotEqualTo(pseudonyms.publicId("whatsapp:+32470000002"));
    }

    @Test
    void thePublicIdIsNotAPrefixOfTheInternalKey() {
        assertThat(pseudonyms.senderKey(PHONE)).doesNotStartWith(pseudonyms.publicId(PHONE).substring(3));
    }

    @Test
    void theSecretChangesEveryId() {
        Pseudonyms other = pseudonyms("second-secret-0123456789");
        assertThat(other.senderKey(PHONE)).isNotEqualTo(pseudonyms.senderKey(PHONE));
        assertThat(other.publicId(PHONE)).isNotEqualTo(pseudonyms.publicId(PHONE));
    }

    private static Pseudonyms pseudonyms(String secret) {
        return new Pseudonyms(TestProperties.with(Map.of("security.pseudonym-secret", secret)));
    }
}
