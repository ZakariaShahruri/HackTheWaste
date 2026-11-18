package be.leuven.leuvengo.security;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SessionTokensTest {

    @Test
    void tokensAreLongRandomAndUrlSafe() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String token = SessionTokens.newToken();
            assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
            assertThat(seen.add(token)).isTrue();
        }
    }

    @Test
    void hashIsStableHexAndNotTheToken() {
        String token = SessionTokens.newToken();
        assertThat(SessionTokens.hash(token))
                .isEqualTo(SessionTokens.hash(token))
                .hasSize(64)
                .matches("[0-9a-f]+")
                .isNotEqualTo(token);
    }
}
