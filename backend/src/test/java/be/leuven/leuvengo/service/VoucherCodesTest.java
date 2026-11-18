package be.leuven.leuvengo.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class VoucherCodesTest {

    @Test
    void codesAreReadableAndVaried() {
        Random random = new Random(7);
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String code = VoucherCodes.generate(random);
            assertThat(code).matches("LG-[A-Z2-9]{4}-[A-Z2-9]{4}");
            assertThat(code.substring(3)).doesNotContainPattern("[01OIL]");
            seen.add(code);
        }
        assertThat(seen).hasSize(1000);
    }
}
