package be.leuven.leuvengo.repository;

import be.leuven.leuvengo.domain.Redemption;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RedemptionRepository extends JpaRepository<Redemption, Long> {

    @EntityGraph(attributePaths = "reward")
    List<Redemption> findByUserIdOrderByRedeemedAtDesc(Long userId);
}
