package be.leuven.leuvengo.service;

/**
 * Who filed a report. {@code pseudoId} is the public, non-identifying id; {@code clan}
 * is the faculty to credit and {@code userId} the account to credit, both optional.
 * Only logged-in browser reports have an account; WhatsApp senders never do.
 */
public record Reporter(String pseudoId, String clan, Long userId) {

    public static Reporter anonymous(String pseudoId, String clan) {
        return new Reporter(pseudoId, clan, null);
    }
}
