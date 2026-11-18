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

    /** Spends points only if the balance covers them. Returns 0 when it doesn't. */
    @Modifying
    @Query("update AppUser u set u.points = u.points - :cost where u.id = :id and u.points >= :cost")
    int spendPoints(long id, int cost);

    @Query("select u.points from AppUser u where u.id = :id")
    int pointsOf(long id);
}
