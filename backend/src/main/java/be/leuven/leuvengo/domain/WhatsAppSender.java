package be.leuven.leuvengo.domain;

import jakarta.persistence.*;

import java.time.Instant;

/** Preferences of a WhatsApp reporter, keyed by pseudonym. Currently just their clan. */
@Entity
@Table(name = "whatsapp_sender")
public class WhatsAppSender {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** HMAC of the sender's number, as in {@link PendingReport}. */
    @Column(nullable = false, unique = true, length = 64)
    private String senderKey;

    @Column(length = 10)
    private String facultyShortCode;

    @Column(nullable = false)
    private Instant updatedAt;

    protected WhatsAppSender() {}

    public WhatsAppSender(String senderKey) {
        this.senderKey = senderKey;
    }

    public String getSenderKey() { return senderKey; }
    public String getFacultyShortCode() { return facultyShortCode; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void joinClan(String facultyShortCode, Instant at) {
        this.facultyShortCode = facultyShortCode;
        this.updatedAt = at;
    }
}
