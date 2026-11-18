package be.leuven.leuvengo.domain;

import jakarta.persistence.*;

import java.time.Instant;

/** A reward a user spent points on, with the voucher code they show to claim it. */
@Entity
@Table(name = "redemption", indexes = @Index(name = "idx_redemption_user", columnList = "user_id"))
public class Redemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private RewardItem reward;

    @Column(nullable = false, unique = true, length = 16)
    private String voucherCode;

    /** What it cost at the time; the reward's price may change later. */
    @Column(nullable = false)
    private int costPoints;

    @Column(nullable = false)
    private Instant redeemedAt;

    protected Redemption() {}

    public Redemption(AppUser user, RewardItem reward, String voucherCode, int costPoints, Instant redeemedAt) {
        this.user = user;
        this.reward = reward;
        this.voucherCode = voucherCode;
        this.costPoints = costPoints;
        this.redeemedAt = redeemedAt;
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public RewardItem getReward() { return reward; }
    public String getVoucherCode() { return voucherCode; }
    public int getCostPoints() { return costPoints; }
    public Instant getRedeemedAt() { return redeemedAt; }
}
