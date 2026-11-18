package be.leuven.leuvengo.repository;

import be.leuven.leuvengo.domain.RewardItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RewardItemRepository extends JpaRepository<RewardItem, Long> {

    /**
     * Takes one item off the shelf if any are left, as a single conditional update,
     * so two people can never both get the last one. Returns 0 when sold out.
     */
    @Modifying
    @Query("update RewardItem r set r.stock = r.stock - 1 where r.id = :id and r.stock > 0")
    int takeOne(long id);
}
