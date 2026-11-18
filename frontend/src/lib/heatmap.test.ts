import { describe, expect, it } from "vitest";
import { heatPixel, likelihoodColor, toHeatGrid } from "./heatmap";

describe("heatPixel", () => {
  it("leaves the model's baseline transparent", () => {
    expect(heatPixel(0.1)[3]).toBe(0);
  });

  it("is red and at full strength for a very likely spot", () => {
    expect(heatPixel(0.9)).toEqual([220, 38, 38, 191]);
  });

  it("gets stronger as the likelihood rises", () => {
    const alphas = [0.15, 0.25, 0.35].map((l) => heatPixel(l)[3]);
    expect(alphas).toEqual([...alphas].sort((a, b) => a - b));
    expect(new Set(alphas).size).toBe(3);
  });

  it("uses the band colour exactly at a band's start", () => {
    expect(heatPixel(0.4).slice(0, 3)).toEqual([249, 115, 22]);
  });
});

describe("likelihoodColor", () => {
  it.each([
    [0.7, "rgb(220, 38, 38)"],
    [0.5, "rgb(249, 115, 22)"],
    [0.05, "rgb(34, 197, 94)"],
  ])("colours %d as %s", (likelihood, color) => {
    expect(likelihoodColor(likelihood)).toBe(color);
  });
});

describe("toHeatGrid", () => {
  const grid = [
    { lat: 50.86, lng: 4.68, likelihood: 0.1 },
    { lat: 50.86, lng: 4.7, likelihood: 0.1 },
    { lat: 50.88, lng: 4.68, likelihood: 0.9 },
    { lat: 50.88, lng: 4.7, likelihood: 0.1 },
  ];

  it("puts the north-west point in the first pixel, whatever the order", () => {
    const heat = toHeatGrid([...grid].reverse())!;

    expect([heat.width, heat.height]).toEqual([2, 2]);
    expect(Array.from(heat.pixels.slice(0, 4))).toEqual(heatPixel(0.9));
    expect(heat.pixels[7]).toBe(0);
  });

  it("extends the bounds half a cell past the outer points", () => {
    const { bounds } = toHeatGrid(grid)!;

    expect(bounds.north).toBeCloseTo(50.89);
    expect(bounds.south).toBeCloseTo(50.85);
    expect(bounds.west).toBeCloseTo(4.67);
    expect(bounds.east).toBeCloseTo(4.71);
  });

  it("needs at least a 2×2 grid", () => {
    expect(toHeatGrid([])).toBeNull();
    expect(toHeatGrid(grid.slice(0, 2))).toBeNull();
  });
});
