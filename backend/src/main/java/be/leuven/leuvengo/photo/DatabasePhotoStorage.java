package be.leuven.leuvengo.photo;

import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

/** Keeps photos in the app's own database and serves them from {@code /api/photos/{id}}. */
public class DatabasePhotoStorage implements PhotoStorage {

    public static final String PATH_PREFIX = "/api/photos/";

    private final StoredPhotoRepository photos;
    private final Clock clock;

    public DatabasePhotoStorage(StoredPhotoRepository photos, Clock clock) {
        this.photos = photos;
        this.clock = clock;
    }

    @Override
    @Transactional
    public String store(ImageNormalizer.Normalized photo, String ownerId) {
        String id = UUID.randomUUID().toString();
        photos.save(new StoredPhoto(id, photo.contentType(), photo.bytes(), clock.instant()));
        return PATH_PREFIX + id;
    }

    @Override
    @Transactional
    public void delete(String url) {
        if (url != null && url.startsWith(PATH_PREFIX)) {
            photos.deleteById(url.substring(PATH_PREFIX.length()));
        }
    }

    @Transactional(readOnly = true)
    public Optional<StoredPhoto> find(String id) {
        return photos.findById(id);
    }

    @Override
    public String describe() {
        return "database";
    }
}
