package be.leuven.leuvengo.photo;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredPhotoRepository extends JpaRepository<StoredPhoto, String> {
}
