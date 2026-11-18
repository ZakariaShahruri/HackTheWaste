package be.leuven.leuvengo.photo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/** A photo kept in the database (the default storage). */
@Entity
@Table(name = "stored_photo")
public class StoredPhoto {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 50)
    private String contentType;

    @JdbcTypeCode(SqlTypes.LONG32VARBINARY)
    @Column(nullable = false)
    private byte[] bytes;

    @Column(nullable = false)
    private Instant createdAt;

    protected StoredPhoto() {}

    public StoredPhoto(String id, String contentType, byte[] bytes, Instant createdAt) {
        this.id = id;
        this.contentType = contentType;
        this.bytes = bytes;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getContentType() { return contentType; }
    public byte[] getBytes() { return bytes; }
    public Instant getCreatedAt() { return createdAt; }
}
