package be.leuven.leuvengo.service;

import java.util.random.RandomGenerator;

/** Voucher codes like {@code LG-7KQ2-M9XD}, meant to be read aloud or typed at a counter. */
public final class VoucherCodes {

    /** No 0/O, 1/I/L: they get misread. */
    static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    private VoucherCodes() {}

    public static String generate(RandomGenerator random) {
        StringBuilder code = new StringBuilder("LG-");
        for (int i = 0; i < 8; i++) {
            if (i == 4) code.append('-');
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
