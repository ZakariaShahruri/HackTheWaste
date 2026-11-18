import { describe, expect, it } from "vitest";
import { SCORE_BANDS, UNSCORED_COLOR } from "./scores";
import { contrast, readableText } from "./colors";

describe("readableText", () => {
  it("puts white on dark colours and dark text on bright ones", () => {
    expect(readableText("#dc2626")).toBe("#ffffff");
    expect(readableText("#0b1220")).toBe("#ffffff");
    expect(readableText("#25D366")).toBe("#0f172a");
    expect(readableText("#facc15")).toBe("#0f172a");
  });

  it("keeps every score colour readable at WCAG AA", () => {
    for (const bg of [...SCORE_BANDS.map((b) => b.color), UNSCORED_COLOR]) {
      expect(contrast(bg, readableText(bg))).toBeGreaterThanOrEqual(4.5);
    }
  });

  it("falls back to white for anything that isn't a hex colour", () => {
    expect(readableText("var(--fg-soft)")).toBe("#ffffff");
  });
});

describe("contrast", () => {
  it("matches the WCAG extremes", () => {
    expect(contrast("#000000", "#ffffff")).toBeCloseTo(21);
    expect(contrast("#777777", "#777777")).toBe(1);
  });
});
