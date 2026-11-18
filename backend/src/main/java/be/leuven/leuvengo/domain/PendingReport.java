package be.leuven.leuvengo.domain;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * A WhatsApp report waiting for its photo: the sender has shared a location
 * pin and we expect a photo next. One per sender; deleted once the photo
 * arrives or after it expires.
 */
@Entity
@Table(name = "pending_report")
public class PendingReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** HMAC of the sender's number, never the number itself. */
    @Column(nullable = false, unique = true, length = 64)
    private String senderKey;

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lng;

    @Column(nullable = false)
    private Instant createdAt;

    protected PendingReport() {}

    public PendingReport(String senderKey, double lat, double lng, Instant createdAt) {
        this.senderKey = senderKey;
        this.lat = lat;
        this.lng = lng;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getSenderKey() { return senderKey; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }
    public Instant getCreatedAt() { return createdAt; }

    public void moveTo(double lat, double lng, Instant at) {
        this.lat = lat;
        this.lng = lng;
        this.createdAt = at;
    }
}
