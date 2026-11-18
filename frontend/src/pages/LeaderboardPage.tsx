import { Link } from "react-router";
import { api } from "../api/client";
import type { LeaderboardEntry } from "../api/types";
import { useApi } from "../hooks/useApi";
import { AsyncContent } from "../components/AsyncContent";
import { EmptyState } from "../components/States";

const MEDALS = ["🥇", "🥈", "🥉"];

export default function LeaderboardPage() {
  const board = useApi((signal) => api.leaderboard(signal));

  return (
    <div className="container">
      <div className="page-header">
        <div>
          <h1 className="page-title">Faculty clans · Leuven Go</h1>
          <div className="muted page-subtitle">
            Every report scores points for your faculty. Cleaner streets win.
          </div>
        </div>
        <Link to="/report" className="btn btn-accent btn-sm">
          📱 Report litter
        </Link>
      </div>

      <section className="card">
        <AsyncContent state={board} empty={<EmptyState icon="🏆" title="No clans yet" />}>
          {(entries) => {
            const max = Math.max(1, ...entries.map((e) => e.points));
            return (
              <ol className="board">
                {entries.map((e, i) => (
                  <BoardRow key={e.id} entry={e} leader={i === 0} share={e.points / max} />
                ))}
              </ol>
            );
          }}
        </AsyncContent>
      </section>
    </div>
  );
}

function BoardRow({ entry: e, leader, share }: { entry: LeaderboardEntry; leader: boolean; share: number }) {
  return (
    <li className={`board-row${leader ? " board-leader" : ""}`}>
      <div className={`board-rank${e.rank <= 3 ? " top" : ""}`}>
        {e.rank <= 3 ? MEDALS[e.rank - 1] : `#${e.rank}`}
      </div>
      <div className="board-emoji" aria-hidden>{e.emoji}</div>
      <div className="board-name">
        <div className="board-title">{e.name}</div>
        <div className="muted board-code">Clan {e.shortCode}</div>
        <div className="board-bar">
          <div style={{ width: `${share * 100}%`, background: e.color }} />
        </div>
      </div>
      <div className="board-points">
        {e.points}
        <div className="muted board-unit">pts</div>
      </div>
    </li>
  );
}
