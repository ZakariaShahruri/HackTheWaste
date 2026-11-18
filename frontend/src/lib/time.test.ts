import { describe, expect, it } from "vitest";
import { timeAgo } from "./time";

const NOW = Date.parse("2026-11-18T12:00:00Z");

describe("timeAgo", () => {
  it.each([
    ["2026-11-18T11:59:30Z", "just now"],
    ["2026-11-18T11:55:00Z", "5m ago"],
    ["2026-11-18T09:00:00Z", "3h ago"],
    ["2026-11-16T11:00:00Z", "2d ago"],
  ])("%s reads as %s", (iso, expected) => {
    expect(timeAgo(iso, NOW)).toBe(expected);
  });
});
