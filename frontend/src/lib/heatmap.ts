import type { PredictPoint } from "../api/types";

type Rgb = readonly [number, number, number];

/** Forecast likelihood bands, highest first. The heatmap shades between their colours. */
export const LIKELIHOOD_BANDS = [
  { min: 0.65, rgb: [220, 38, 38], label: "Very high" },
  { min: 0.4, rgb: [249, 115, 22], label: "High" },
  { min: 0.25, rgb: [234, 179, 8], label: "Moderate" },
  { min: 0.15, rgb: [132, 204, 22], label: "Low" },
  { min: 0, rgb: [34, 197, 94], label: "Clean" },
] as const satisfies readonly { min: number; rgb: Rgb; label: string }[];

export function cssColor(rgb: Rgb): string {
  return `rgb(${rgb.join(", ")})`;
}

export function likelihoodColor(likelihood: number): string {
  return cssColor((LIKELIHOOD_BANDS.find((b) => likelihood >= b.min) ?? LIKELIHOOD_BANDS[4]).rgb);
}

// The model's baseline is about 0.10 everywhere; only what rises above it is drawn.
const INVISIBLE_BELOW = 0.12;
const FULLY_VISIBLE_AT = 0.4;
const MAX_ALPHA = 0.75;

/** RGBA for a likelihood: transparent at the baseline, a smooth band colour above it. */
export function heatPixel(likelihood: number): [number, number, number, number] {
  const [r, g, b] = shade(likelihood);
  const visible = (likelihood - INVISIBLE_BELOW) / (FULLY_VISIBLE_AT - INVISIBLE_BELOW);
  const alpha = Math.min(1, Math.max(0, visible)) * MAX_ALPHA;
  return [Math.round(r), Math.round(g), Math.round(b), Math.round(alpha * 255)];
}

/** The colour between the two bands the likelihood falls between. */
function shade(likelihood: number): Rgb {
  const ascending = [...LIKELIHOOD_BANDS].reverse();
  for (let i = 0; i < ascending.length - 1; i++) {
    const from = ascending[i];
    const to = ascending[i + 1];
    if (likelihood < to.min) {
      const f = Math.max(0, (likelihood - from.min) / (to.min - from.min));
      const mix = (c: number) => from.rgb[c] + f * (to.rgb[c] - from.rgb[c]);
      return [mix(0), mix(1), mix(2)];
    }
  }
  return LIKELIHOOD_BANDS[0].rgb;
}

export interface HeatGrid {
  width: number;
  height: number;
  /** Row-major RGBA, north row first, west column first. */
  pixels: Uint8ClampedArray<ArrayBuffer>;
  /** Outer edges of the cells, so each grid point sits at its pixel's centre. */
  bounds: { north: number; south: number; west: number; east: number };
}

/** Turns a regular forecast grid into one RGBA pixel per grid point. */
export function toHeatGrid(points: PredictPoint[]): HeatGrid | null {
  const lats = [...new Set(points.map((p) => p.lat))].sort((a, b) => b - a);
  const lngs = [...new Set(points.map((p) => p.lng))].sort((a, b) => a - b);
  if (lats.length < 2 || lngs.length < 2) return null;

  const row = new Map(lats.map((lat, i) => [lat, i]));
  const col = new Map(lngs.map((lng, i) => [lng, i]));
  const pixels = new Uint8ClampedArray(lats.length * lngs.length * 4);
  for (const p of points) {
    pixels.set(heatPixel(p.likelihood), (row.get(p.lat)! * lngs.length + col.get(p.lng)!) * 4);
  }

  const halfLat = (lats[0] - lats[lats.length - 1]) / (lats.length - 1) / 2;
  const halfLng = (lngs[lngs.length - 1] - lngs[0]) / (lngs.length - 1) / 2;
  return {
    width: lngs.length,
    height: lats.length,
    pixels,
    bounds: {
      north: lats[0] + halfLat,
      south: lats[lats.length - 1] - halfLat,
      west: lngs[0] - halfLng,
      east: lngs[lngs.length - 1] + halfLng,
    },
  };
}
