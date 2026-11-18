package be.leuven.leuvengo.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 254)
    private String email;

    @Column(nullable = false, length = 72)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role = Role.USER;

    /** Optional clan chosen at signup; their reports credit this faculty. */
    @Column(length = 10)
    private String facultyShortCode;

    /**
     * SHA-256 of the current bearer token. The token itself is only ever sent
     * to the client, so a database leak doesn't hand out live sessions.
     */
    @Column(length = 64, unique = true)
    private String sessionTokenHash;

    private Instant sessionExpiresAt;

    /** Whether the user gave explicit data-use consent at signup. */
    @Column(nullable = false)
    private boolean consentGiven = false;

    /** When consent was recorded — kept for GDPR accountability. */
    private Instant consentGivenAt;

    @Column(nullable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getFacultyShortCode() { return facultyShortCode; }
    public void setFacultyShortCode(String facultyShortCode) { this.facultyShortCode = facultyShortCode; }
    public String getSessionTokenHash() { return sessionTokenHash; }
    public void setSessionTokenHash(String sessionTokenHash) { this.sessionTokenHash = sessionTokenHash; }
    public Instant getSessionExpiresAt() { return sessionExpiresAt; }
    public void setSessionExpiresAt(Instant sessionExpiresAt) { this.sessionExpiresAt = sessionExpiresAt; }
    public boolean isConsentGiven() { return consentGiven; }
    public void setConsentGiven(boolean consentGiven) { this.consentGiven = consentGiven; }
    public Instant getConsentGivenAt() { return consentGivenAt; }
    public void setConsentGivenAt(Instant consentGivenAt) { this.consentGivenAt = consentGivenAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
