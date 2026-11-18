package be.leuven.leuvengo.web;

import be.leuven.leuvengo.domain.AppUser;
import be.leuven.leuvengo.repository.RewardItemRepository;
import be.leuven.leuvengo.security.CurrentUser;
import be.leuven.leuvengo.service.RewardService;
import be.leuven.leuvengo.web.dto.Dtos;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rewards")
public class RewardController {

    private final RewardItemRepository rewards;
    private final RewardService rewardService;

    public RewardController(RewardItemRepository rewards, RewardService rewardService) {
        this.rewards = rewards;
        this.rewardService = rewardService;
    }

    public record RedeemResponse(Dtos.RedemptionDto redemption, int pointsLeft) {}

    @GetMapping
    public List<Dtos.RewardItemDto> list() {
        return rewards.findAll().stream().map(Dtos::of).toList();
    }

    @PostMapping("/{id}/redeem")
    @ResponseStatus(HttpStatus.CREATED)
    public RedeemResponse redeem(@PathVariable long id, @CurrentUser AppUser user) {
        RewardService.Redeemed redeemed = rewardService.redeem(user, id);
        return new RedeemResponse(Dtos.of(redeemed.redemption()), redeemed.pointsLeft());
    }

    @GetMapping("/mine")
    public List<Dtos.RedemptionDto> mine(@CurrentUser AppUser user) {
        return rewardService.redeemedBy(user).stream().map(Dtos::of).toList();
    }
}
