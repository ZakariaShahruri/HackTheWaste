package be.leuven.leuvengo.domain;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Half a WhatsApp report: either the location pin or the photo, waiting for the
 * other. One per sender; deleted once the report is filed or after it expires.
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

    private Double lat;

    private Double lng;

    /** Where Twilio keeps the photo; it is only downloaded once there's a pin. */
    @Column(length = 1024)
    private String mediaUrl;

    @Column(nullable = false)
    private Instant createdAt;

    protected PendingReport() {}

    public PendingReport(String senderKey) {
        this.senderKey = senderKey;
    }

    public Long getId() { return id; }
    public String getSenderKey() { return senderKey; }
    public Double getLat() { return lat; }
    public Double getLng() { return lng; }
    public String getMediaUrl() { return mediaUrl; }
    public Instant getCreatedAt() { return createdAt; }

    public void holdPin(double lat, double lng, Instant at) {
        this.lat = lat;
        this.lng = lng;
        this.mediaUrl = null;
        this.createdAt = at;
    }

    public void holdPhoto(String mediaUrl, Instant at) {
        this.lat = null;
        this.lng = null;
        this.mediaUrl = mediaUrl;
        this.createdAt = at;
    }
}
