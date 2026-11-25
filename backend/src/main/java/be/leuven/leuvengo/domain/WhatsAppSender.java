package be.leuven.leuvengo.domain;

import jakarta.persistence.*;

import java.time.Instant;

/** A WhatsApp reporter's clan, keyed by pseudonym. */
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

    /** When they were last shown the clan list; null once they picked or moved on. */
    private Instant choosingSince;

    @Column(nullable = false)
    private Instant updatedAt;

    protected WhatsAppSender() {}

    public WhatsAppSender(String senderKey) {
        this.senderKey = senderKey;
    }

    public String getSenderKey() { return senderKey; }
    public String getFacultyShortCode() { return facultyShortCode; }
    public Instant getChoosingSince() { return choosingSince; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void joinClan(String facultyShortCode, Instant at) {
        this.facultyShortCode = facultyShortCode;
        stopChoosing(at);
    }

    public void startChoosing(Instant at) {
        this.choosingSince = at;
        this.updatedAt = at;
    }

    public void stopChoosing(Instant at) {
        this.choosingSince = null;
        this.updatedAt = at;
    }
}
