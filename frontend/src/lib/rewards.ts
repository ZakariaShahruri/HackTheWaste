import type { AuthUser, RewardItemDto } from "../api/types";

/** Why this user can't redeem this reward right now, or null if they can. */
export function redeemBlocker(reward: RewardItemDto, user: AuthUser | null): string | null {
  if (reward.stock <= 0) return "Sold out";
  if (!user) return "Log in to redeem";
  const missing = reward.costPoints - user.points;
  if (missing > 0) return `${missing} more pts needed`;
  return null;
}
