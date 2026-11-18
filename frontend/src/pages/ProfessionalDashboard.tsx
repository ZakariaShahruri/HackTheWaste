import { useMemo, useState } from "react";
import { Link } from "react-router";
import { api } from "../api/client";
import { useApi } from "../hooks/useApi";
import { HotspotMap } from "../components/Map/HotspotMap";
import { PredictiveMap } from "../components/Map/PredictiveMap";
import { Sparkbar } from "../components/Sparkbar";
import { ScoreChip } from "../components/ScoreChip";
import { StatusTag } from "../components/StatusTag";
import { AsyncContent } from "../components/AsyncContent";
import { EmptyState } from "../components/States";
import { FacultySwatch } from "../components/FacultySwatch";

const POLL = { pollMs: 5000 };

export default function ProfessionalDashboard() {
  const stats = useApi((signal) => api.stats(signal), POLL);
  const hotspots = useApi((signal) => api.hotspots(signal), POLL);
  const segments = useApi((signal) => api.segments(signal), POLL);
  const orders = useApi((signal) => api.workOrders(signal), POLL);
  const [district, setDistrict] = useState<string>("ALL");
  const [mapMode, setMapMode] = useState<"live" | "predictive">("predictive");

  const districts = useMemo(() => {
    const set = new Set<string>();
    segments.data?.forEach((s) => s.district && set.add(s.district));
    return ["ALL", ...Array.from(set)];
  }, [segments.data]);

  return (
    <div className="container">
      <div className="page-header">
        <div>
          <h1 className="page-title">Operations Overview</h1>
          <div className="muted page-subtitle">
            Live cleanliness signals across Leuven · last refresh {new Date().toLocaleTimeString()}
          </div>
        </div>
        <Link to="/pro/operations" className="btn btn-ghost btn-sm">
          Operations queue
        </Link>
      </div>

      <section className="kpi-grid">
        <Kpi label="Total reports (72h)" value={stats.data?.totalReports ?? "—"} sub="anonymized signals" />
        <Kpi label="Active hotspots" value={stats.data?.openHotspots ?? "—"} sub="post-clustering" />
        <Kpi
          label="Planon orders dispatched"
          value={stats.data?.dispatchedOrders ?? "—"}
          sub={`${stats.data?.completedOrders ?? 0} completed`}
        />
        <Kpi label="City AI cleanliness" value={stats.data?.avgCleanliness?.toFixed(1) ?? "—"} sub="0–100 weighted score" />
      </section>

      <section className="card section">
        <div className="card-header">
          <div>
            <div className="card-title">
              {mapMode === "live" ? "Live hotspot map" : "Predictive heatmap · ML forecast"}
            </div>
            <h3 className="card-heading">
              {mapMode === "live" ? "Where to send crews now" : "Where trash will accumulate — pick a day & time"}
            </h3>
          </div>
          <div className="tabbar">
            <button className={mapMode === "predictive" ? "active" : ""} onClick={() => setMapMode("predictive")}>
              Predictive
            </button>
            <button className={mapMode === "live" ? "active" : ""} onClick={() => setMapMode("live")}>
              Live hotspots
            </button>
          </div>
        </div>
        {mapMode === "live" ? <HotspotMap hotspots={hotspots.data ?? []} /> : <PredictiveMap />}
      </section>

      <section className="split">
        <div className="card">
          <div className="card-header">
            <div className="card-title">Reports trend (7d)</div>
            <span className="muted table-sub">ingested signals</span>
          </div>
          <Sparkbar data={stats.data?.reportsLast7Days ?? []} />
        </div>

        <div className="card">
          <div className="card-title">Top clusters</div>
          <div style={{ marginTop: 8 }}>
            <AsyncContent
              state={stats}
              isEmpty={(s) => s.topHotspots.length === 0}
              empty={<EmptyState title="All clear ✨" />}
            >
              {(s) => (
                <div className="stack">
                  {s.topHotspots.slice(0, 5).map((h) => (
                    <div key={h.id} className="spread">
                      <div>
                        <div style={{ fontWeight: 600 }}>{h.label ?? "Hotspot"}</div>
                        <div className="muted table-sub">
                          {h.reportCount} reports · cleanliness {Math.round(h.cleanliness)}/100
                        </div>
                      </div>
                      <StatusTag value={h.status} />
                    </div>
                  ))}
                </div>
              )}
            </AsyncContent>
          </div>
        </div>
      </section>

      <section className="card section">
        <div className="card-header">
          <div>
            <div className="card-title">AI cleanliness scores by segment</div>
            <h3 className="card-heading">Streets ranked by current need</h3>
          </div>
          <div className="tabbar">
            {districts.map((d) => (
              <button key={d} className={d === district ? "active" : ""} onClick={() => setDistrict(d)}>
                {d === "ALL" ? "All districts" : d}
              </button>
            ))}
          </div>
        </div>
        <AsyncContent state={segments} empty={<EmptyState title="No streets scored yet" />}>
          {(list) => (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Segment</th>
                    <th>District</th>
                    <th>Reports (72h)</th>
                    <th>Held by</th>
                    <th>AI score</th>
                  </tr>
                </thead>
                <tbody>
                  {list
                    .filter((s) => district === "ALL" || s.district === district)
                    .sort((a, b) => a.aiCleanlinessScore - b.aiCleanlinessScore)
                    .map((s) => (
                      <tr key={s.id}>
                        <td><strong>{s.name}</strong></td>
                        <td className="muted">{s.district ?? "—"}</td>
                        <td>{s.recentReportCount ?? 0}</td>
                        <td><FacultySwatch name={s.facultyName} color={s.facultyColor} /></td>
                        <td><ScoreChip score={s.aiCleanlinessScore ?? 0} /></td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </div>
          )}
        </AsyncContent>
      </section>

      <section className="card section">
        <div className="card-header">
          <div>
            <div className="card-title">Recent Planon work orders</div>
            <h3 className="card-heading">Auto-dispatched from clustered reports</h3>
          </div>
          <Link to="/pro/operations" className="btn btn-ghost btn-sm">
            See all
          </Link>
        </div>
        <AsyncContent
          state={orders}
          empty={<EmptyState title="No work orders yet">Three reports at one spot dispatch a crew.</EmptyState>}
        >
          {(list) => (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Planon ref</th>
                    <th>Summary</th>
                    <th>Crew</th>
                    <th>Priority</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {list.slice(0, 6).map((w) => (
                    <tr key={w.id}>
                      <td className="mono">{w.planonRef ?? "PENDING"}</td>
                      <td>{w.summary}</td>
                      <td>{w.crew ?? "—"}</td>
                      <td><StatusTag value={w.priority} /></td>
                      <td><StatusTag value={w.status} /></td>
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

function Kpi({ label, value, sub }: { label: string; value: string | number; sub: string }) {
  return (
    <div className="card">
      <div className="card-title">{label}</div>
      <div className="kpi-value">{value}</div>
      <div className="kpi-sub">{sub}</div>
    </div>
  );
}
