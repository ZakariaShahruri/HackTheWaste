package be.leuven.leuvengo.photo;

/** Where report photos live. Implementations return a URL the frontend can show. */
public interface PhotoStorage {

    /**
     * Stores an already-normalised JPEG.
     *
     * @param ownerId pseudonymous id of the reporter, used only for organising files
     * @return an absolute URL, or a path starting with {@code /api/} that is served by this backend
     */
    String store(ImageNormalizer.Normalized photo, String ownerId);

    /** Removes a photo by the URL {@link #store} returned; unknown URLs are ignored. */
    void delete(String url);

    /** Short name for startup logs, e.g. "database". */
    String describe();
}
