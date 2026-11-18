import { filled } from "../lib/colors";
import { scoreColor } from "../lib/scores";

export function ScoreChip({ score }: { score: number | null }) {
  if (score === null) {
    return (
      <span className="score-chip" style={filled(scoreColor(null))} title="Not scored yet">
        —
      </span>
    );
  }
  return (
    <span className="score-chip" style={filled(scoreColor(score))}>
      {score.toFixed(0)}
    </span>
  );
}
