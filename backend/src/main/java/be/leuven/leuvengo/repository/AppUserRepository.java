package be.leuven.leuvengo.repository;

import be.leuven.leuvengo.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findBySessionTokenHash(String sessionTokenHash);

    boolean existsByEmail(String email);

    /** Atomic, so concurrent reports and redemptions can't lose an update. */
    @Modifying
    @Query("update AppUser u set u.points = u.points + :points where u.id = :id")
    int addPoints(long id, int points);
}
