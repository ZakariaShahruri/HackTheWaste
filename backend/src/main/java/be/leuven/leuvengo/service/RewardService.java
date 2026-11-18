package be.leuven.leuvengo.service;

import be.leuven.leuvengo.domain.AppUser;
import be.leuven.leuvengo.domain.Redemption;
import be.leuven.leuvengo.domain.RewardItem;
import be.leuven.leuvengo.repository.AppUserRepository;
import be.leuven.leuvengo.repository.RedemptionRepository;
import be.leuven.leuvengo.repository.RewardItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.List;

/** Spending personal points on marketplace rewards. */
@Service
public class RewardService {

    /** The redemption plus the balance left over, so the UI can update without refetching. */
    public record Redeemed(Redemption redemption, int pointsLeft) {}

    private final RewardItemRepository rewards;
    private final AppUserRepository users;
    private final RedemptionRepository redemptions;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public RewardService(RewardItemRepository rewards, AppUserRepository users,
                         RedemptionRepository redemptions, Clock clock) {
        this.rewards = rewards;
        this.users = users;
        this.redemptions = redemptions;
        this.clock = clock;
    }

    /**
     * Stock and points are each taken with a conditional update, and any failure
     * rolls back both, so a sold-out reward never costs points and a short
     * balance never takes stock.
     */
    @Transactional
    public Redeemed redeem(AppUser user, long rewardId) {
        RewardItem reward = rewards.findById(rewardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such reward"));
        int cost = reward.getCostPoints();

        if (rewards.takeOne(rewardId) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sorry, that reward is sold out");
        }
        if (users.spendPoints(user.getId(), cost) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You need " + cost + " points for this reward");
        }

        Redemption redemption = redemptions.save(new Redemption(users.getReferenceById(user.getId()), reward,
                VoucherCodes.generate(random), cost, clock.instant()));
        return new Redeemed(redemption, users.pointsOf(user.getId()));
    }

    @Transactional(readOnly = true)
    public List<Redemption> redeemedBy(AppUser user) {
        return redemptions.findByUserIdOrderByRedeemedAtDesc(user.getId());
    }
}
