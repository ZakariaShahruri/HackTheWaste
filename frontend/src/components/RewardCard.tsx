import { useState } from "react";
import type { RewardItemDto } from "../api/types";

interface Props {
  reward: RewardItemDto;
  /** Why it can't be redeemed right now; null when it can. */
  blocker: string | null;
  busy: boolean;
  onRedeem: (reward: RewardItemDto) => void;
}

/** A marketplace reward. Redeeming takes a second click to confirm, since it spends points. */
export function RewardCard({ reward, blocker, busy, onRedeem }: Props) {
  const [confirming, setConfirming] = useState(false);

  return (
    <article className="card">
      <div className="reward-art" style={{ background: gradientFor(reward.id) }} aria-hidden>
        {iconFor(reward.category)}
      </div>
      <div className="spread" style={{ marginTop: 14 }}>
        <span className="tag">{reward.category}</span>
        <span className="reward-stock">{reward.stock > 0 ? `${reward.stock} left` : "sold out"}</span>
      </div>
      <h2 style={{ fontSize: 18, marginTop: 8 }}>{reward.title}</h2>
      <p className="muted" style={{ fontSize: 13, marginTop: 6 }}>
        {reward.description}
      </p>
      <div className="spread" style={{ marginTop: 14 }}>
        <span className="reward-sponsor">by {reward.sponsor}</span>
        {confirming ? (
          <div className="row" style={{ gap: 6 }}>
            <button className="btn btn-ghost btn-sm" onClick={() => setConfirming(false)} disabled={busy}>
              Cancel
            </button>
            <button
              className="btn btn-accent btn-sm"
              disabled={busy}
              onClick={() => {
                setConfirming(false);
                onRedeem(reward);
              }}
            >
              Spend {reward.costPoints} pts
            </button>
          </div>
        ) : (
          <button
            className={`btn btn-sm reward-action ${blocker ? "btn-ghost" : "btn-accent"}`}
            disabled={blocker !== null || busy}
            title={blocker ?? undefined}
            onClick={() => setConfirming(true)}
          >
            {busy ? "Redeeming…" : blocker ?? `Redeem · ${reward.costPoints} pts`}
          </button>
        )}
      </div>
    </article>
  );
}

function gradientFor(seed: number): string {
  const palettes = [
    "linear-gradient(135deg, #f59e0b, #fb923c)",
    "linear-gradient(135deg, #2563eb, #38bdf8)",
    "linear-gradient(135deg, #16a34a, #22d3ee)",
    "linear-gradient(135deg, #9333ea, #ec4899)",
    "linear-gradient(135deg, #f43f5e, #fb7185)",
    "linear-gradient(135deg, #0891b2, #14b8a6)",
  ];
  return palettes[seed % palettes.length];
}

function iconFor(category: string): string {
  const map: Record<string, string> = {
    Drinks: "🍺",
    Food: "🍱",
    Transport: "🚲",
    Study: "📚",
    Entertainment: "🎟",
    Experience: "🏛",
    Merch: "👕",
  };
  return map[category] ?? "🎁";
}
