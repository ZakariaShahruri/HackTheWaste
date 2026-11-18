import { useMemo, useState } from "react";
import { Link } from "react-router";
import { api } from "../api/client";
import { errorMessage } from "../api/http";
import type { RedemptionDto, RewardItemDto } from "../api/types";
import { RewardCard } from "../components/RewardCard";
import { VoucherList } from "../components/VoucherList";
import { useAuth } from "../context/auth";
import { useApi } from "../hooks/useApi";
import { redeemBlocker } from "../lib/rewards";

export default function MarketplacePage() {
  const { user, setPoints } = useAuth();
  const rewards = useApi((signal) => api.rewards(signal));
  const mine = useApi(
    (signal) => (user ? api.myRedemptions(signal) : Promise.resolve([] as RedemptionDto[])),
    { key: user?.id },
  );
  const [category, setCategory] = useState("ALL");
  const [busyId, setBusyId] = useState<number | null>(null);
  const [justRedeemed, setJustRedeemed] = useState<RedemptionDto | null>(null);
  const [redeemError, setRedeemError] = useState<string | null>(null);

  const categories = useMemo(() => {
    const set = new Set<string>();
    rewards.data?.forEach((r) => set.add(r.category));
    return ["ALL", ...Array.from(set).sort()];
  }, [rewards.data]);

  const visible = (rewards.data ?? []).filter(
    (r) => category === "ALL" || r.category === category,
  );

  const redeem = async (reward: RewardItemDto) => {
    setBusyId(reward.id);
    setRedeemError(null);
    setJustRedeemed(null);
    try {
      const result = await api.redeemReward(reward.id);
      setPoints(result.pointsLeft);
      setJustRedeemed(result.redemption);
      rewards.refetch();
      mine.refetch();
    } catch (e) {
      setRedeemError(errorMessage(e));
      rewards.refetch();
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="container">
      <div className="spread" style={{ flexWrap: "wrap", gap: 12 }}>
        <div>
          <h1 style={{ fontSize: 28 }}>Reward marketplace</h1>
          <div className="muted" style={{ marginTop: 4 }}>
            {user
              ? `You have ${user.points} pts. Every report you send while logged in earns more.`
              : "Log in and report litter to earn points, then spend them on Leuven perks."}
          </div>
        </div>
        <Link to="/student" className="btn btn-ghost btn-sm">
          Back to map
        </Link>
      </div>

      {justRedeemed && (
        <div role="status" className="card redeem-success">
          <div>
            <div style={{ fontWeight: 700 }}>{justRedeemed.title} is yours</div>
            <div className="muted" style={{ fontSize: 13 }}>Show this code at {justRedeemed.sponsor}.</div>
          </div>
          <code className="voucher-code">{justRedeemed.voucherCode}</code>
        </div>
      )}
      {redeemError && (
        <div role="alert" className="tag tag-danger" style={{ marginTop: 16 }}>
          {redeemError}
        </div>
      )}

      <div className="tabbar" style={{ marginTop: 16 }}>
        {categories.map((c) => (
          <button
            key={c}
            className={c === category ? "active" : ""}
            onClick={() => setCategory(c)}
          >
            {c === "ALL" ? "Everything" : c}
          </button>
        ))}
      </div>

      <section
        className="grid"
        style={{
          gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))",
          marginTop: 16,
        }}
      >
        {visible.map((r) => (
          <RewardCard
            key={r.id}
            reward={r}
            blocker={redeemBlocker(r, user)}
            busy={busyId === r.id}
            onRedeem={(reward) => void redeem(reward)}
          />
        ))}
      </section>

      {user && (mine.data?.length ?? 0) > 0 && (
        <section style={{ marginTop: 32 }}>
          <h2 style={{ fontSize: 20 }}>My rewards</h2>
          <VoucherList redemptions={mine.data ?? []} />
        </section>
      )}
    </div>
  );
}
