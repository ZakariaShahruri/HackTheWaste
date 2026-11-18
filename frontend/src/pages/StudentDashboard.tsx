import { useMemo, useState } from "react";
import { Link } from "react-router";
import { api } from "../api/client";
import type { LeaderboardEntry } from "../api/types";
import { useApi } from "../hooks/useApi";
import { StudentMap } from "../components/Map/StudentMap";
import { ScoreChip } from "../components/ScoreChip";
import { byNeed, cleanest } from "../lib/scores";
import { AsyncContent } from "../components/AsyncContent";
import { EmptyState } from "../components/States";
import { FacultySwatch } from "../components/FacultySwatch";

const MEDALS = ["🥇", "🥈", "🥉"];
const RANKINGS_SHOWN = 8;
const POLL = { pollMs: 8000 };

export default function StudentDashboard() {
  const faculties = useApi((signal) => api.faculties(signal), POLL);
  const segments = useApi((signal) => api.segments(signal), POLL);
  const hotspots = useApi((signal) => api.hotspots(signal), POLL);
  const leaderboard = useApi((signal) => api.leaderboard(signal), POLL);
  const [selected, setSelected] = useState<string | undefined>(undefined);

  const myFaculty = selected ? faculties.data?.find((f) => f.shortCode === selected) : undefined;

  const myRank = useMemo(() => {
    if (!leaderboard.data || !myFaculty) return null;
    return leaderboard.data.find((e) => e.shortCode === myFaculty.shortCode)?.rank;
  }, [leaderboard.data, myFaculty]);

  const toggle = (code: string) => setSelected((s) => (s === code ? undefined : code));

  return (
    <div className="container">
      <section className="card student-hero">
        <div className="student-hero-title">
          <span className="student-eyebrow">🎓 Leuven Go · student crew</span>
          <h1>{myFaculty ? `${myFaculty.emoji} ${myFaculty.name}` : "Pick your clan, claim your streets"}</h1>
          {myFaculty && myRank && (
            <div className="student-hero-rank">
              Rank #{myRank} · {myFaculty.points} pts
            </div>
          )}
        </div>

        <div className="clan-picker" role="group" aria-label="Pick a clan">
          {(faculties.data ?? []).map((f) => {
            const active = selected === f.shortCode;
            return (
              <button
                key={f.id}
                type="button"
                className={`clan-chip${active ? " active" : ""}`}
                aria-pressed={active}
                title={f.name}
                onClick={() => toggle(f.shortCode)}
                style={{ ["--clan" as string]: f.color }}
              >
                <span className="clan-chip-emoji" aria-hidden>{f.emoji}</span>
                <span className="clan-chip-code">{f.shortCode}</span>
              </button>
            );
          })}
        </div>

        <div className="stat-grid">
          <Stat label="Clan rank" value={myRank ? `#${myRank}` : "—"} sub={myFaculty?.name ?? "select a clan"} />
          <Stat label="Clan points" value={myFaculty?.points ?? 0} sub="for the clan leaderboard" />
          <Stat
            label="Active hotspots"
            value={(hotspots.data ?? []).filter((h) => h.status !== "RESOLVED").length}
            sub="across Leuven"
          />
          <Stat
            label="Cleanest street"
            value={cleanest(segments.data ?? [])?.name ?? "—"}
            sub="goals for everyone"
          />
        </div>
      </section>

      <section className="split">
        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">Faculty territories</div>
              <h2 className="card-heading">
                {myFaculty ? `${myFaculty.emoji} ${myFaculty.name}'s patch` : "Tap a clan to highlight their patch"}
              </h2>
            </div>
            <Link to="/student/leaderboard" className="btn btn-ghost btn-sm">
              Full leaderboard →
            </Link>
          </div>
          <StudentMap
            faculties={faculties.data ?? []}
            segments={segments.data ?? []}
            hotspots={hotspots.data ?? []}
            selectedFaculty={selected}
          />
        </div>

        <div className="card">
          <div className="card-header">
            <div className="card-title">Clan rankings</div>
          </div>
          <AsyncContent state={leaderboard} empty={<EmptyState title="No clans yet" />}>
            {(entries) => (
              <div className="stack">
                {entries
                  .filter((e) => e.rank <= RANKINGS_SHOWN || e.shortCode === selected)
                  .map((e) => (
                    <RankingRow
                      key={e.id}
                      entry={e}
                      selected={e.shortCode === selected}
                      onToggle={() => toggle(e.shortCode)}
                    />
                  ))}
              </div>
            )}
          </AsyncContent>
        </div>
      </section>

      <section className="card section">
        <div className="card-header">
          <div>
            <div className="card-title">Streets to clean up</div>
            <h2 className="card-heading">Lowest scoring spots near you</h2>
          </div>
          <Link to="/market" className="btn btn-accent btn-sm">🛍 Spend points</Link>
        </div>
        <AsyncContent state={segments} empty={<EmptyState title="No streets scored yet" />}>
          {(list) => (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Street</th>
                    <th>Held by</th>
                    <th>Reports (72h)</th>
                    <th>Score</th>
                  </tr>
                </thead>
                <tbody>
                  {[...list]
                    .sort(byNeed)
                    .slice(0, 6)
                    .map((s) => (
                      <tr key={s.id}>
                        <td>
                          <strong>{s.name}</strong>
                          <div className="muted table-sub">{s.district}</div>
                        </td>
                        <td><FacultySwatch name={s.facultyName} color={s.facultyColor} /></td>
                        <td>{s.recentReportCount}</td>
                        <td><ScoreChip score={s.aiCleanlinessScore} /></td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </div>
          )}
        </AsyncContent>
      </section>
    </div>
  );
}

function RankingRow({ entry: e, selected, onToggle }: { entry: LeaderboardEntry; selected: boolean; onToggle: () => void }) {
  return (
    <button
      type="button"
      className={`lb-row${selected ? " me" : ""}`}
      aria-pressed={selected}
      onClick={onToggle}
      style={{ borderColor: selected ? e.color : undefined }}
    >
      <span className={`lb-rank${e.rank <= 3 ? " top" : ""}`}>{e.rank <= 3 ? MEDALS[e.rank - 1] : e.rank}</span>
      <span className="lb-emoji" aria-hidden>{e.emoji}</span>
      <span className="lb-name">
        <strong>{e.name}</strong>
        <span className="muted">Clan {e.shortCode}</span>
      </span>
      <span className="lb-points">{e.points}</span>
    </button>
  );
}

function Stat({ label, value, sub }: { label: string; value: string | number; sub: string }) {
  return (
    <div className="stat">
      <div className="card-title">{label}</div>
      <div className="stat-value">{value}</div>
      <div className="muted stat-sub">{sub}</div>
    </div>
  );
}
