package be.leuven.leuvengo.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * A single citizen / student report. GDPR-friendly:
 * - no personal identifiers are persisted (only an anonymous pseudo-id)
 * - image is referenced by hash/url, not stored as raw bytes here
 * - only derived metrics are kept long term
 */
@Entity
@Table(name = "report", indexes = {
        @Index(name = "idx_report_geo", columnList = "lat,lng"),
        @Index(name = "idx_report_hotspot", columnList = "hotspot_id"),
        @Index(name = "idx_report_reported_at", columnList = "reportedAt")
})
public class Report {

    /** How the report came in; also decides how the reporter is shown in the feed. */
    public enum Source { WHATSAPP, WEB, DEMO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    /** 0-100, see {@link CleanlinessScale}. */
    @Column(nullable = false)
    private Integer cleanlinessScore;

    /** Free-text observation, e.g. "overflowing bin", "broken glass". */
    @Column(length = 500)
    private String note;

    /** Where the photo is served from (see PhotoStorage); null for reports without a photo. */
    @Column(length = 1024)
    private String photoUrl;

    /** One-sentence assessment from the AI inspector, if it gave one. */
    @Column(length = 200)
    private String aiDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Source source;

    /** Waste types spotted, comma separated (plastic, glass, ...). */
    @Column(length = 200)
    private String signalTags;

    /** Pseudonymous reporter id (e.g. an HMAC of the phone number), never PII. */
    private String reporterPseudoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id")
    private Faculty creditedFaculty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotspot_id")
    private Hotspot hotspot;

    @Column(nullable = false)
    private Instant reportedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }
    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }
    public Integer getCleanlinessScore() { return cleanlinessScore; }
    public void setCleanlinessScore(Integer v) { this.cleanlinessScore = v; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public String getAiDescription() { return aiDescription; }
    public void setAiDescription(String aiDescription) { this.aiDescription = aiDescription; }
    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }
    public String getSignalTags() { return signalTags; }
    public void setSignalTags(String s) { this.signalTags = s; }
    public String getReporterPseudoId() { return reporterPseudoId; }
    public void setReporterPseudoId(String v) { this.reporterPseudoId = v; }
    public Faculty getCreditedFaculty() { return creditedFaculty; }
    public void setCreditedFaculty(Faculty f) { this.creditedFaculty = f; }
    public Hotspot getHotspot() { return hotspot; }
    public void setHotspot(Hotspot hotspot) { this.hotspot = hotspot; }
    public Instant getReportedAt() { return reportedAt; }
    public void setReportedAt(Instant reportedAt) { this.reportedAt = reportedAt; }
}
