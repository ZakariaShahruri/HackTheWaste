import { Link } from "react-router";
import { useApi } from "../hooks/useApi";
import { api } from "../api/client";
import { AsyncContent } from "../components/AsyncContent";
import { PhotoCard } from "../components/PhotoCard";
import { EmptyState, Skeleton } from "../components/States";
import { WhatsAppReportCTA } from "../components/WhatsAppReportCTA";

export default function LandingPage() {
  const POLL = { pollMs: 10000 };
  const { data: stats } = useApi((signal) => api.stats(signal), POLL);
  const { data: leaderboard } = useApi((signal) => api.leaderboard(signal), POLL);
  const top = leaderboard?.[0];

  const feed = useApi((signal) => api.feed(0, 6, undefined, signal));

  return (
    <div className="container landing">
      <section className="landing-hero">
        <span className="landing-eyebrow">Stad Leuven · Hack the Waste 2026</span>
        <h1>
          One photo. One tap.
          <br />A cleaner Leuven.
        </h1>
        <p>Cleanliness signals from the street, dispatched to the right Planon crew.</p>
        <WhatsAppReportCTA variant="inline" className="landing-hero-cta" />
      </section>

      <section className="landing-stats card">
        <Stat label="Reports" value={stats?.totalReports ?? "—"} />
        <Sep />
        <Stat label="Live hotspots" value={stats?.openHotspots ?? "—"} />
        <Sep />
        <Stat
          label="City avg cleanliness"
          value={stats?.avgCleanliness?.toFixed(1) ?? "—"}
          suffix="/100"
        />
        <Sep />
        <Stat
          label="Top clan"
          value={
            top ? (
              <span>
                {top.emoji} {top.shortCode}
              </span>
            ) : (
              "—"
            )
          }
          sub={top ? `${top.points} pts` : undefined}
        />
      </section>

      <section className="landing-choice">
        <Link to="/pro" className="landing-card landing-card-pro">
          <div className="landing-tag">For city teams</div>
          <h2>Personnel</h2>
          <p>
            Heatmap, AI cleanliness scores per street segment, and Planon work-order
            monitoring.
          </p>
          <span className="landing-arrow">→</span>
        </Link>

        <Link to="/student" className="landing-card landing-card-student">
          <div className="landing-tag">For students</div>
          <h2>Students</h2>
          <p>
            Pick your clan, claim your streets, climb the 24 Uur leaderboard, redeem
            rewards.
          </p>
          <span className="landing-arrow">→</span>
        </Link>
      </section>

      {/* Live feed */}
      <section style={{ marginTop: 48 }}>
        <div className="spread" style={{ marginBottom: 16, alignItems: "flex-end" }}>
          <div>
            <h2 style={{ fontSize: 22, margin: 0 }}>Latest reports</h2>
            <p className="muted" style={{ fontSize: 13, marginTop: 4 }}>
              Real-time trash reports from across Leuven
            </p>
          </div>
          <Link to="/feed" style={{ fontSize: 14, color: "var(--primary)", fontWeight: 600 }}>
            See all →
          </Link>
        </div>

        <AsyncContent
          state={feed}
          isEmpty={(page) => page.content.length === 0}
          empty={
            <div className="card">
              <EmptyState icon="🗑" title="No reports yet">
                Send one on WhatsApp or from your browser to see it here.
              </EmptyState>
            </div>
          }
          loading={<div className="feed-grid"><Skeleton count={6} className="feed-card feed-skeleton" /></div>}
        >
          {(page) => (
            <div className="feed-grid">
              {page.content.map((p) => (
                <PhotoCard key={p.id} photo={p} />
              ))}
            </div>
          )}
        </AsyncContent>
      </section>
    </div>
  );
}

function Stat({
  label,
  value,
  sub,
  suffix,
}: {
  label: string;
  value: React.ReactNode;
  sub?: string;
  suffix?: string;
}) {
  return (
    <div className="landing-stat">
      <div className="landing-stat-label">{label}</div>
      <div className="landing-stat-value">
        {value}
        {suffix && <span className="landing-stat-suffix">{suffix}</span>}
      </div>
      {sub && <div className="landing-stat-sub">{sub}</div>}
    </div>
  );
}

function Sep() {
  return <div className="landing-stat-sep" aria-hidden />;
}
