import { useState } from "react";
import { api } from "../api/client";
import { errorMessage } from "../api/http";
import type { WorkOrderDto } from "../api/types";
import { useApi } from "../hooks/useApi";
import { StatusTag } from "../components/StatusTag";
import { HotspotMap } from "../components/Map/HotspotMap";
import { AsyncContent } from "../components/AsyncContent";
import { EmptyState } from "../components/States";

export default function OperationsPage() {
  const orders = useApi((signal) => api.workOrders(signal));
  const hotspots = useApi((signal) => api.activeHotspots(signal));
  const [busyId, setBusyId] = useState<number | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const complete = async (id: number) => {
    setBusyId(id);
    setActionError(null);
    try {
      await api.completeWorkOrder(id);
      orders.refetch();
      hotspots.refetch();
    } catch (e) {
      setActionError(errorMessage(e, "Could not complete the work order"));
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="container">
      <div className="page-header">
        <div>
          <h1 className="page-title">Crew operations queue</h1>
          <div className="muted page-subtitle">
            Dispatched via Planon · routed by district and cleanliness
          </div>
        </div>
      </div>

      <section className="split split-even">
        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">Active hotspots</div>
              <h2 className="card-heading">Targeted routing for cleaning crews & machinery</h2>
            </div>
          </div>
          <HotspotMap hotspots={hotspots.data ?? []} />
        </div>

        <div className="card">
          <div className="card-title">Work order queue</div>
          {actionError && (
            <div role="alert" className="tag tag-danger" style={{ marginTop: 10 }}>
              {actionError}
            </div>
          )}
          <div style={{ marginTop: 12 }}>
            <AsyncContent
              state={orders}
              empty={<EmptyState icon="✅" title="No work orders">Hotspots that reach three reports show up here.</EmptyState>}
            >
              {(list) => (
                <div className="stack">
                  {list.map((w) => (
                    <WorkOrderCard
                      key={w.id}
                      order={w}
                      busy={busyId === w.id}
                      onComplete={() => void complete(w.id)}
                    />
                  ))}
                </div>
              )}
            </AsyncContent>
          </div>
        </div>
      </section>
    </div>
  );
}

function WorkOrderCard({ order: w, busy, onComplete }: { order: WorkOrderDto; busy: boolean; onComplete: () => void }) {
  return (
    <article className="work-order">
      <div className="spread work-order-head">
        <div>
          <div className="mono work-order-ref">{w.planonRef ?? `WO-${w.id}`}</div>
          <div className="muted work-order-time">{new Date(w.createdAt).toLocaleString("nl-BE")}</div>
        </div>
        <div className="row work-order-tags">
          <StatusTag value={w.priority} />
          <StatusTag value={w.status} />
        </div>
      </div>
      <div className="work-order-summary">{w.summary}</div>
      <div className="muted work-order-time">Crew: {w.crew ?? "unassigned"}</div>
      {w.status !== "COMPLETED" && (
        <button className="btn btn-primary btn-sm work-order-action" disabled={busy} onClick={onComplete}>
          {busy ? "Marking…" : "Mark completed"}
        </button>
      )}
    </article>
  );
}
