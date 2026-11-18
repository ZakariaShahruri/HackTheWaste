package be.leuven.leuvengo.photo;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.BlobUrlParts;
import com.azure.storage.blob.models.BlobHttpHeaders;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/** Stores photos in an Azure Blob container; used when AZURE_STORAGE_CONNECTION_STRING is set. */
public class AzureBlobPhotoStorage implements PhotoStorage {

    private final BlobContainerClient container;
    private final Clock clock;

    public AzureBlobPhotoStorage(String connectionString, String containerName, Clock clock) {
        this.container = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient()
                .getBlobContainerClient(containerName);
        this.clock = clock;
    }

    @Override
    public String store(ImageNormalizer.Normalized photo, String ownerId) {
        // {date}/{ownerId}/{uuid}.jpg keeps blobs easy to list and partition.
        String name = LocalDate.now(clock) + "/" + ownerId + "/" + UUID.randomUUID() + ".jpg";
        BlobClient blob = container.getBlobClient(name);
        blob.upload(new ByteArrayInputStream(photo.bytes()), photo.bytes().length, true);
        blob.setHttpHeaders(new BlobHttpHeaders().setContentType(photo.contentType()));
        return blob.getBlobUrl();
    }

    @Override
    public void delete(String url) {
        BlobUrlParts parts = BlobUrlParts.parse(url);
        if (container.getBlobContainerName().equals(parts.getBlobContainerName())) {
            container.getBlobClient(parts.getBlobName()).deleteIfExists();
        }
    }

    @Override
    public String describe() {
        return "azure blob (" + container.getBlobContainerName() + ")";
    }
}
