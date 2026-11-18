import { describe, expect, it } from "vitest";
import { UNSCORED_COLOR, byNeed, cleanest, scoreColor } from "./scores";

const street = (name: string, aiCleanlinessScore: number | null) => ({ name, aiCleanlinessScore });

describe("byNeed", () => {
  it("puts the dirtiest street first and unscored streets last", () => {
    const streets = [street("new", null), street("clean", 90), street("dirty", 31), street("ok", 66)];

    expect([...streets].sort(byNeed).map((s) => s.name)).toEqual(["dirty", "ok", "clean", "new"]);
  });
});

describe("cleanest", () => {
  it("ignores unscored streets", () => {
    expect(cleanest([street("new", null), street("clean", 90), street("dirty", 31)])?.name).toBe("clean");
  });

  it("is undefined when nothing is scored yet", () => {
    expect(cleanest([street("new", null)])).toBeUndefined();
  });
});

describe("scoreColor", () => {
  it.each([
    [100, "#16a34a"],
    [80, "#16a34a"],
    [79.9, "#f59e0b"],
    [45, "#f97316"],
    [0, "#dc2626"],
  ])("colours %d as %s", (score, color) => {
    expect(scoreColor(score)).toBe(color);
  });

  it("greys out a street without a score", () => {
    expect(scoreColor(null)).toBe(UNSCORED_COLOR);
  });
});
