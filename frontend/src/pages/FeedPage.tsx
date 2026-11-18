import { useEffect, useState } from "react";
import { api } from "../api/client";
import { errorMessage } from "../api/http";
import { useApi } from "../hooks/useApi";
import { PhotoCard } from "../components/PhotoCard";
import type { FeedItemDto } from "../api/types";

const PAGE_SIZE = 12;

export default function FeedPage() {
  const faculties = useApi((signal) => api.faculties(signal));
  const [clan, setClan] = useState<string | undefined>(undefined);
  const [total, setTotal] = useState<number | null>(null);

  return (
    <div className="container" style={{ maxWidth: 1100 }}>
      <div className="spread" style={{ marginBottom: 20 }}>
        <div>
          <h1 style={{ fontSize: 28 }}>Leuven Feed</h1>
          <div className="muted" style={{ marginTop: 4, fontSize: 14 }}>
            {total ?? "—"} trash reports captured across Leuven
          </div>
        </div>
      </div>

      {/* Clan filter */}
      <div style={{ display: "flex", flexWrap: "wrap", gap: 6, marginBottom: 20 }}>
        <button
          className="btn btn-sm"
          style={{
            background: !clan ? "var(--fg)" : "white",
            color: !clan ? "white" : "var(--fg)",
            border: "1px solid var(--border)",
          }}
          onClick={() => setClan(undefined)}
        >
          All clans
        </button>
        {(faculties.data ?? []).map((f) => (
          <button
            key={f.id}
            className="btn btn-sm"
            style={{
              background: clan === f.shortCode ? f.color : "white",
              color: clan === f.shortCode ? "white" : "#1f2937",
              border: `1px solid ${f.color}`,
              fontWeight: 600,
            }}
            onClick={() => setClan((c) => (c === f.shortCode ? undefined : f.shortCode))}
          >
            {f.emoji} {f.shortCode}
          </button>
        ))}
      </div>

      {/* Remounting per clan resets paging state without effects juggling it. */}
      <FeedResults key={clan ?? "all"} clan={clan} onTotal={setTotal} />
    </div>
  );
}

function FeedResults({ clan, onTotal }: { clan?: string; onTotal: (n: number) => void }) {
  const [pages, setPages] = useState(1);
  const [photos, setPhotos] = useState<FeedItemDto[]>([]);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    api
      .feed(pages - 1, PAGE_SIZE, clan, controller.signal)
      .then((result) => {
        setPhotos((prev) => [...prev, ...result.content]);
        setHasMore(!result.last);
        onTotal(result.totalElements);
        setError(null);
      })
      .catch((e: unknown) => {
        if (!controller.signal.aborted) setError(errorMessage(e, "Could not load the feed"));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [pages, clan, onTotal]);

  function loadMore() {
    setLoading(true);
    setPages((n) => n + 1);
  }

  if (error && photos.length === 0) {
    return (
      <div className="card" style={{ textAlign: "center", padding: 48, color: "var(--fg-soft)" }}>
        <div style={{ fontWeight: 600 }}>{error}</div>
      </div>
    );
  }

  if (photos.length === 0 && !loading) {
    return (
      <div className="card" style={{ textAlign: "center", padding: 48, color: "var(--fg-soft)" }}>
        <div style={{ fontSize: 48, marginBottom: 12 }}>🗑</div>
        <div style={{ fontWeight: 600 }}>No reports yet for this filter</div>
        <div style={{ fontSize: 14, marginTop: 6 }}>
          Send a report via WhatsApp or try it in your browser to see photos here.
        </div>
      </div>
    );
  }

  return (
    <>
      <div className="feed-grid">
        {photos.map((p) => (
          <PhotoCard key={p.id} photo={p} />
        ))}
        {loading &&
          Array.from({ length: 6 }).map((_, i) => (
            <div key={`sk-${i}`} className="feed-card feed-skeleton" />
          ))}
      </div>

      {hasMore && !loading && photos.length > 0 && (
        <div style={{ textAlign: "center", marginTop: 32 }}>
          <button className="btn btn-ghost" onClick={loadMore}>
            Load more
          </button>
        </div>
      )}
    </>
  );
}
