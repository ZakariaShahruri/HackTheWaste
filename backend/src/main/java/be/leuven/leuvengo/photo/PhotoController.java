package be.leuven.leuvengo.photo;

import be.leuven.leuvengo.web.error.NotFoundException;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/** Serves photos kept by {@link DatabasePhotoStorage}. Ids are random UUIDs, and content never changes. */
@RestController
@RequestMapping("/api/photos")
public class PhotoController {

    private final PhotoStorage storage;

    public PhotoController(PhotoStorage storage) {
        this.storage = storage;
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> get(@PathVariable String id) {
        if (!(storage instanceof DatabasePhotoStorage db) || !id.matches("[0-9a-f-]{36}")) {
            throw new NotFoundException("Photo", id);
        }
        StoredPhoto photo = db.find(id).orElseThrow(() -> new NotFoundException("Photo", id));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .header("X-Content-Type-Options", "nosniff")
                .body(photo.getBytes());
    }
}
