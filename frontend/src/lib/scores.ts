/** Cleanliness bands on the 0–100 scale (100 = clean), cleanest first. */
export const SCORE_BANDS = [
  { min: 80, color: "#16a34a", label: "80–100 sparkling" },
  { min: 60, color: "#f59e0b", label: "60–80 ok" },
  { min: 40, color: "#f97316", label: "40–60 grimy" },
  { min: 0, color: "#dc2626", label: "0–40 needs love" },
] as const;

export const UNSCORED_COLOR = "#94a3b8";

export function scoreColor(score: number | null | undefined): string {
  if (score == null) return UNSCORED_COLOR;
  return SCORE_BANDS.find((band) => score >= band.min)?.color ?? SCORE_BANDS[SCORE_BANDS.length - 1].color;
}

interface Scored {
  aiCleanlinessScore: number | null;
}

/** Dirtiest first, so the streets that need a crew lead; unscored streets go last. */
export function byNeed(a: Scored, b: Scored): number {
  if (a.aiCleanlinessScore === null) return b.aiCleanlinessScore === null ? 0 : 1;
  if (b.aiCleanlinessScore === null) return -1;
  return a.aiCleanlinessScore - b.aiCleanlinessScore;
}

/** The cleanest scored street, if any street has a score yet. */
export function cleanest<T extends Scored>(streets: T[]): T | undefined {
  let best: T | undefined;
  for (const s of streets) {
    if (s.aiCleanlinessScore === null) continue;
    if (!best || s.aiCleanlinessScore > (best.aiCleanlinessScore ?? -Infinity)) best = s;
  }
  return best;
}
