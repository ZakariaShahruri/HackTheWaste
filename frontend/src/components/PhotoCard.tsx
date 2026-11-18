import { useState } from "react";
import { assetUrl } from "../api/http";
import type { FeedItemDto } from "../api/types";
import { filled } from "../lib/colors";
import { timeAgo } from "../lib/time";
import { ScoreChip } from "./ScoreChip";

/** One report in the photo feed: the photo, who sent it, where, and the AI's verdict. */
export function PhotoCard({ photo: p }: { photo: FeedItemDto }) {
  const [imgError, setImgError] = useState(false);
  const tags = p.tags?.split(",").map((t) => t.trim()).filter(Boolean) ?? [];

  return (
    <article className="feed-card">
      {p.photoUrl && !imgError ? (
        <img
          src={assetUrl(p.photoUrl)}
          alt={`Litter report at ${p.segmentName ?? "Leuven"}`}
          className="feed-photo"
          onError={() => setImgError(true)}
          loading="lazy"
        />
      ) : (
        <div className="feed-placeholder" aria-hidden>🗑</div>
      )}

      <div className="feed-meta">
        <div className="spread feed-heading">
          <div className="feed-who">
            {p.facultyShortCode && (
              <span className="feed-clan" style={filled(p.facultyColor ?? "var(--fg-soft)")}>
                {p.facultyEmoji} {p.facultyShortCode}
              </span>
            )}
            <div className="feed-username">{p.username}</div>
            {p.segmentName && <div className="feed-location">📍 {p.segmentName}</div>}
          </div>
          {p.cleanlinessScore != null ? (
            <ScoreChip score={p.cleanlinessScore} />
          ) : (
            <span className="feed-pending">AI pending</span>
          )}
        </div>

        {p.description && <p className="feed-description">{p.description}</p>}

        {tags.length > 0 && (
          <div className="feed-tags">
            {tags.map((t) => (
              <span key={t} className="feed-tag">#{t}</span>
            ))}
          </div>
        )}

        <time className="feed-time" dateTime={p.reportedAt}>{timeAgo(p.reportedAt)}</time>
      </div>
    </article>
  );
}
