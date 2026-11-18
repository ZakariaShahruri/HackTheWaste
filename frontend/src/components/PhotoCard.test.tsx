import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { FeedItemDto } from "../api/types";
import { PhotoCard } from "./PhotoCard";

const report: FeedItemDto = {
  id: 1,
  username: "Anonymous Leuvenaar",
  facultyShortCode: "ENG",
  facultyEmoji: "⚙️",
  facultyColor: "#2563eb",
  photoUrl: "/api/photos/abc",
  segmentName: "Oude Markt",
  cleanlinessScore: 32,
  tags: "cans, cups,",
  description: "Plastic cups and cans around the benches.",
  reportedAt: new Date(Date.now() - 5 * 60_000).toISOString(),
};

describe("PhotoCard", () => {
  it("shows the photo, place, score, assessment and tags", () => {
    render(<PhotoCard photo={report} />);

    expect(screen.getByRole("img", { name: "Litter report at Oude Markt" })).toHaveAttribute("src", "/api/photos/abc");
    expect(screen.getByText("📍 Oude Markt")).toBeInTheDocument();
    expect(screen.getByText("32")).toBeInTheDocument();
    expect(screen.getByText("Plastic cups and cans around the benches.")).toBeInTheDocument();
    expect(screen.getAllByText(/^#/).map((t) => t.textContent)).toEqual(["#cans", "#cups"]);
    expect(screen.getByText("5m ago")).toBeInTheDocument();
  });

  it("says the AI is still pending when there's no score", () => {
    render(<PhotoCard photo={{ ...report, cleanlinessScore: undefined }} />);

    expect(screen.getByText("AI pending")).toBeInTheDocument();
  });

  it("falls back to a placeholder when the photo doesn't load", () => {
    render(<PhotoCard photo={report} />);

    fireEvent.error(screen.getByRole("img"));

    expect(screen.queryByRole("img")).not.toBeInTheDocument();
  });
});
