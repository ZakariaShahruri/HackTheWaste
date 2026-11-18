import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import type { RewardItemDto } from "../api/types";
import { RewardCard } from "./RewardCard";

const reward: RewardItemDto = {
  id: 7, title: "Stadsmagazijn Tour", description: "Behind the scenes", sponsor: "City of Leuven",
  category: "Experience", costPoints: 30, stock: 12,
};

describe("RewardCard", () => {
  it("asks for confirmation before spending points", async () => {
    const onRedeem = vi.fn();
    render(<RewardCard reward={reward} blocker={null} busy={false} onRedeem={onRedeem} />);

    await userEvent.click(screen.getByRole("button", { name: "Redeem · 30 pts" }));
    expect(onRedeem).not.toHaveBeenCalled();

    await userEvent.click(screen.getByRole("button", { name: "Spend 30 pts" }));
    expect(onRedeem).toHaveBeenCalledWith(reward);
  });

  it("can back out of the confirmation", async () => {
    const onRedeem = vi.fn();
    render(<RewardCard reward={reward} blocker={null} busy={false} onRedeem={onRedeem} />);

    await userEvent.click(screen.getByRole("button", { name: "Redeem · 30 pts" }));
    await userEvent.click(screen.getByRole("button", { name: "Cancel" }));

    expect(screen.getByRole("button", { name: "Redeem · 30 pts" })).toBeEnabled();
    expect(onRedeem).not.toHaveBeenCalled();
  });

  it("shows why it can't be redeemed instead of a dead button", () => {
    render(<RewardCard reward={reward} blocker="15 more pts needed" busy={false} onRedeem={vi.fn()} />);

    expect(screen.getByRole("button", { name: "15 more pts needed" })).toBeDisabled();
  });
});
