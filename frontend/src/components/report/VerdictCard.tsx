import { Link } from "react-router";
import { assetUrl } from "../../api/http";
import type { DemoReportResult } from "../../api/types";
import { ScoreChip } from "../ScoreChip";

interface Props {
  result: DemoReportResult;
  /** Personal points the report earned, when the reporter is logged in. */
  earnedPoints?: number;
  onAnother: () => void;
}

/** What the AI made of a photo sent from the browser. */
export function VerdictCard({ result, earnedPoints, onAnother }: Props) {
  return (
    <section className="verdict" aria-live="polite">
      <img className="verdict-photo" src={assetUrl(result.photoUrl)} alt="The photo you sent" />
      <div className="verdict-body">
        <div className="card-title">Report filed</div>
        <div className="row verdict-score">
          <ScoreChip score={result.aiAvailable ? result.score : null} />
          <strong>{result.label}</strong>
        </div>
        {result.aiAvailable ? (
          result.description && <p className="verdict-description">{result.description}</p>
        ) : (
          <p className="muted verdict-description">
            The AI inspector couldn't be reached, so the photo hasn't been scored. The report still
            counts towards the hotspot.
          </p>
        )}
        {result.tags.length > 0 && (
          <div className="feed-tags">
            {result.tags.map((t) => (
              <span key={t} className="feed-tag">#{t}</span>
            ))}
          </div>
        )}
        {earnedPoints !== undefined && earnedPoints > 0 && (
          <p className="verdict-points">+{earnedPoints} pts for you</p>
        )}
        <div className="row verdict-actions">
          <Link to="/student" className="btn btn-primary btn-sm">See it on the map</Link>
          <Link to="/feed" className="btn btn-ghost btn-sm">Open the feed</Link>
          <button type="button" className="link-btn" onClick={onAnother}>Report another</button>
        </div>
      </div>
    </section>
  );
}
