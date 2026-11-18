package be.leuven.leuvengo.web;

import be.leuven.leuvengo.domain.RewardItem;
import be.leuven.leuvengo.repository.AppUserRepository;
import be.leuven.leuvengo.repository.RedemptionRepository;
import be.leuven.leuvengo.repository.RewardItemRepository;
import be.leuven.leuvengo.security.AuthService;
import be.leuven.leuvengo.service.RewardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RewardRedeemIT {

    private static final AtomicInteger USER = new AtomicInteger();

    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @Autowired AppUserRepository users;
    @Autowired RewardItemRepository rewards;
    @Autowired RedemptionRepository redemptions;
    @Autowired RewardService rewardService;
    @Autowired TransactionTemplate transaction;

    @Test
    void redeemingSpendsPointsTakesStockAndHandsOutAVoucher() throws Exception {
        RewardItem reward = reward(30, 5);
        AuthService.Session alice = userWith(100);

        mvc.perform(redeem(reward, alice))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.redemption.title").value(reward.getTitle()))
                .andExpect(jsonPath("$.redemption.costPoints").value(30))
                .andExpect(jsonPath("$.redemption.voucherCode").value(org.hamcrest.Matchers.matchesPattern("LG-.{4}-.{4}")))
                .andExpect(jsonPath("$.pointsLeft").value(70));

        assertThat(stockOf(reward)).isEqualTo(4);
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(alice)))
                .andExpect(jsonPath("$.points").value(70));
        mvc.perform(get("/api/rewards/mine").header("Authorization", bearer(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].rewardId").value(reward.getId()));
    }

    @Test
    void aShortBalanceIsRefusedAndTakesNoStock() throws Exception {
        RewardItem reward = reward(80, 5);
        AuthService.Session bob = userWith(79);

        mvc.perform(redeem(reward, bob))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You need 80 points for this reward"));

        assertThat(stockOf(reward)).isEqualTo(5);
        assertThat(users.pointsOf(bob.user().getId())).isEqualTo(79);
    }

    @Test
    void aSoldOutRewardCostsNothing() throws Exception {
        RewardItem reward = reward(10, 0);
        AuthService.Session carol = userWith(50);

        mvc.perform(redeem(reward, carol))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Sorry, that reward is sold out"));

        assertThat(users.pointsOf(carol.user().getId())).isEqualTo(50);
    }

    @Test
    void unknownRewardsAndLoggedOutVisitorsAreTurnedAway() throws Exception {
        AuthService.Session dave = userWith(50);

        mvc.perform(post("/api/rewards/999999/redeem").header("Authorization", bearer(dave)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/rewards/" + reward(10, 5).getId() + "/redeem"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/rewards/mine")).andExpect(status().isUnauthorized());
    }

    @Test
    void twoPeopleRacingForTheLastOneCantBothGetIt() throws Exception {
        RewardItem reward = reward(10, 1);
        AuthService.Session first = userWith(50);
        AuthService.Session second = userWith(50);

        List<HttpStatus> outcomes = race(
                () -> rewardService.redeem(first.user(), reward.getId()),
                () -> rewardService.redeem(second.user(), reward.getId()));

        assertThat(outcomes).containsExactlyInAnyOrder(HttpStatus.CREATED, HttpStatus.CONFLICT);
        assertThat(stockOf(reward)).isZero();
        assertThat(users.pointsOf(first.user().getId()) + users.pointsOf(second.user().getId())).isEqualTo(90);
    }

    @Test
    void oneBalanceCantBeSpentTwice() throws Exception {
        // Two different rewards, so only the points guard stands between them.
        RewardItem hoodie = reward(40, 10);
        RewardItem ticket = reward(40, 10);
        AuthService.Session eve = userWith(50);

        List<HttpStatus> outcomes = race(
                () -> rewardService.redeem(eve.user(), hoodie.getId()),
                () -> rewardService.redeem(eve.user(), ticket.getId()));

        assertThat(outcomes).containsExactlyInAnyOrder(HttpStatus.CREATED, HttpStatus.BAD_REQUEST);
        assertThat(users.pointsOf(eve.user().getId())).isEqualTo(10);
        assertThat(stockOf(hoodie) + stockOf(ticket)).isEqualTo(19);
        assertThat(redemptions.findByUserIdOrderByRedeemedAtDesc(eve.user().getId())).hasSize(1);
    }

    /** Starts both calls at the same moment and reports how each one ended. */
    private static List<HttpStatus> race(Callable<?> a, Callable<?> b) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            List<Future<HttpStatus>> futures = new ArrayList<>();
            for (Callable<?> call : List.of(a, b)) {
                futures.add(pool.submit(() -> {
                    start.await();
                    try {
                        call.call();
                        return HttpStatus.CREATED;
                    } catch (ResponseStatusException e) {
                        return HttpStatus.valueOf(e.getStatusCode().value());
                    }
                }));
            }
            start.countDown();
            List<HttpStatus> outcomes = new ArrayList<>();
            for (Future<HttpStatus> f : futures) outcomes.add(f.get());
            return outcomes;
        }
    }

    private RewardItem reward(int cost, int stock) {
        RewardItem r = new RewardItem();
        r.setTitle("Test reward " + USER.incrementAndGet());
        r.setCostPoints(cost);
        r.setStock(stock);
        return rewards.save(r);
    }

    private AuthService.Session userWith(int points) {
        AuthService.Session s = auth.register("redeem-" + USER.incrementAndGet() + "@example.be",
                "secret-pass", null, true);
        transaction.executeWithoutResult(t -> users.addPoints(s.user().getId(), points));
        return s;
    }

    private int stockOf(RewardItem reward) {
        return rewards.findById(reward.getId()).orElseThrow().getStock();
    }

    private static org.springframework.test.web.servlet.RequestBuilder redeem(RewardItem reward, AuthService.Session s) {
        return post("/api/rewards/" + reward.getId() + "/redeem").header("Authorization", bearer(s));
    }

    private static String bearer(AuthService.Session s) {
        return "Bearer " + s.token();
    }
}
