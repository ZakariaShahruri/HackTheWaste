import { describe, expect, it } from "vitest";
import type { AuthUser, RewardItemDto } from "../api/types";
import { redeemBlocker } from "./rewards";

const reward: RewardItemDto = {
  id: 1, title: "Velo day pass", description: "", sponsor: "Velo", category: "Transport",
  costPoints: 60, stock: 3,
};
const user = (points: number): AuthUser => ({ id: 1, email: "a@b.c", role: "USER", points, token: "t" });

describe("redeemBlocker", () => {
  it("lets a user with enough points redeem", () => {
    expect(redeemBlocker(reward, user(60))).toBeNull();
  });

  it("says how many points are missing", () => {
    expect(redeemBlocker(reward, user(45))).toBe("15 more pts needed");
  });

  it("asks visitors to log in", () => {
    expect(redeemBlocker(reward, null)).toBe("Log in to redeem");
  });

  it("puts sold out first, since nobody can get it", () => {
    expect(redeemBlocker({ ...reward, stock: 0 }, null)).toBe("Sold out");
    expect(redeemBlocker({ ...reward, stock: 0 }, user(500))).toBe("Sold out");
  });
});
