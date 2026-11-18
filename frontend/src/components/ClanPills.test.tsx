import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import type { FacultyDto } from "../api/types";
import { ClanPills } from "./ClanPills";

const faculties: FacultyDto[] = [
  { id: 1, name: "Ekonomika", shortCode: "EKO", color: "#10b981", emoji: "📊", members: 0, points: 0 },
  { id: 2, name: "VTK", shortCode: "VTK", color: "#dc2626", emoji: "⚙️", members: 0, points: 0 },
];

describe("ClanPills", () => {
  it("selects a clan, and clears it when clicked again", async () => {
    const onSelect = vi.fn();
    const { rerender } = render(<ClanPills label="Clan" faculties={faculties} onSelect={onSelect} />);

    await userEvent.click(screen.getByRole("button", { name: "📊 EKO" }));
    expect(onSelect).toHaveBeenLastCalledWith("EKO");

    rerender(<ClanPills label="Clan" faculties={faculties} selected="EKO" onSelect={onSelect} />);
    expect(screen.getByRole("button", { name: "📊 EKO" })).toHaveAttribute("aria-pressed", "true");
    await userEvent.click(screen.getByRole("button", { name: "📊 EKO" }));
    expect(onSelect).toHaveBeenLastCalledWith(undefined);
  });

  it("offers a pill that clears the filter", async () => {
    const onSelect = vi.fn();
    render(<ClanPills label="Filter" faculties={faculties} selected="VTK" onSelect={onSelect} noneLabel="All clans" />);

    expect(screen.getByRole("button", { name: "All clans" })).toHaveAttribute("aria-pressed", "false");
    await userEvent.click(screen.getByRole("button", { name: "All clans" }));
    expect(onSelect).toHaveBeenCalledWith(undefined);
  });
});
