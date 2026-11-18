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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class BlobStorageService {

    private static final Logger log = LoggerFactory.getLogger(BlobStorageService.class);

    private final String connectionString;
    private final String containerName;
    private final String twilioAccountSid;
    private final String twilioAuthToken;

    public BlobStorageService(LeuvenGoProperties props) {
        this.connectionString = props.storage().azureConnectionString();
        this.containerName = props.storage().azureContainer();
        this.twilioAccountSid = props.twilio().accountSid();
        this.twilioAuthToken = props.twilio().authToken();
    }

    private BlobContainerClient containerClient;

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

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
     * Downloads the image from {@code twilioUrl}, uploads it to the trash-photos
     * container, and returns the public blob URL. Returns empty if storage is
     * not configured or the upload fails.
     *
     * @param pseudoId  GDPR-safe user identifier used to organise blobs by path
     */
    public Optional<String> uploadFromTwilio(String twilioUrl, String mimeType, String pseudoId) {
        if (containerClient == null) {
            log.warn("Blob storage not configured — skipping upload for {}", twilioUrl);
            return Optional.empty();
        }
        try {
            byte[] bytes = downloadFromTwilio(twilioUrl);
            String ext = mimeType.contains("png") ? "png" : mimeType.contains("gif") ? "gif" : "jpg";
            // Path: {date}/{pseudoId}/{uuid}.{ext}  — easy to list and partition for ML exports
            String blobName = LocalDate.now() + "/" + pseudoId + "/" + UUID.randomUUID() + "." + ext;

            BlobClient blob = containerClient.getBlobClient(blobName);
            blob.upload(new ByteArrayInputStream(bytes), bytes.length, true);
            blob.setHttpHeaders(new BlobHttpHeaders().setContentType(mimeType));

            String url = blob.getBlobUrl();
            log.info("Uploaded blob: {} ({} bytes)", url, bytes.length);
            return Optional.of(url);
        } catch (Exception e) {
            log.error("Blob upload failed for {}: {}", twilioUrl, e.getMessage());
            return Optional.empty();
        }
    }

    private byte[] downloadFromTwilio(String url) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url)).GET();
        if (url.contains("api.twilio.com") && !twilioAccountSid.isBlank()) {
            String creds = twilioAccountSid + ":" + twilioAuthToken;
            builder.header("Authorization",
                    "Basic " + Base64.getEncoder().encodeToString(creds.getBytes()));
        }
        HttpResponse<byte[]> resp = http.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() >= 300) {
            throw new RuntimeException("Twilio download failed: HTTP " + resp.statusCode());
        }
        return resp.body();
    }
}
