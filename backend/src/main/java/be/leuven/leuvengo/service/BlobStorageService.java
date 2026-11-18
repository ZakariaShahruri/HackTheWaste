package be.leuven.leuvengo.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import be.leuven.leuvengo.config.LeuvenGoProperties;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class BlobStorageService {

    private static final Logger log = LoggerFactory.getLogger(BlobStorageService.class);

    private final String connectionString;
    private final String containerName;

    public BlobStorageService(LeuvenGoProperties props) {
        this.connectionString = props.storage().azureConnectionString();
        this.containerName = props.storage().azureContainer();
    }

    private BlobContainerClient containerClient;

    @PostConstruct
    void init() {
        if (connectionString == null || connectionString.isBlank()) {
            log.warn("AZURE_STORAGE_CONNECTION_STRING not configured — blob uploads will be skipped");
            return;
        }
        containerClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient()
                .getBlobContainerClient(containerName);
        log.info("Azure Blob Storage ready: container={}", containerName);
    }

    /**
     * Uploads a photo and returns its public blob URL, or empty if storage
     * isn't configured or the upload fails.
     *
     * @param pseudoId  GDPR-safe user identifier used to organise blobs by path
     */
    public Optional<String> upload(byte[] bytes, String mimeType, String pseudoId) {
        if (containerClient == null) {
            return Optional.empty();
        }
        try {
            String ext = switch (mimeType) {
                case "image/png" -> "png";
                case "image/webp" -> "webp";
                case "image/heic", "image/heif" -> "heic";
                default -> "jpg";
            };
            // {date}/{pseudoId}/{uuid}.{ext}: easy to list and partition for ML exports
            String blobName = LocalDate.now() + "/" + pseudoId + "/" + UUID.randomUUID() + "." + ext;

            BlobClient blob = containerClient.getBlobClient(blobName);
            blob.upload(new ByteArrayInputStream(bytes), bytes.length, true);
            blob.setHttpHeaders(new BlobHttpHeaders().setContentType(mimeType));

            log.info("Uploaded blob {} ({} bytes)", blobName, bytes.length);
            return Optional.of(blob.getBlobUrl());
        } catch (RuntimeException e) {
            log.error("Blob upload failed: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
