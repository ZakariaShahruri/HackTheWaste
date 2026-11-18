package be.leuven.leuvengo.photo;

import be.leuven.leuvengo.config.LeuvenGoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Picks the photo store: Azure when a connection string is configured, the database otherwise. */
@Configuration
public class PhotoStorageConfig {

    private static final Logger log = LoggerFactory.getLogger(PhotoStorageConfig.class);

    @Bean
    PhotoStorage photoStorage(LeuvenGoProperties props, StoredPhotoRepository photos, Clock clock) {
        LeuvenGoProperties.Storage storage = props.storage();
        PhotoStorage chosen = storage.azureConnectionString().isBlank()
                ? new DatabasePhotoStorage(photos, clock)
                : new AzureBlobPhotoStorage(storage.azureConnectionString(), storage.azureContainer(), clock);
        log.info("Photo storage: {}", chosen.describe());
        return chosen;
    }
}
