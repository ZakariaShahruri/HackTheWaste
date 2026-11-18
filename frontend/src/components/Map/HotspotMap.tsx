import { CircleMarker, Popup } from "react-leaflet";
import type { HotspotDto } from "../../api/types";
import { MapBase } from "./MapBase";

const STATUS_COLOR: Record<HotspotDto["status"], string> = {
  OPEN: "#f97316",
  ESCALATED: "#dc2626",
  DISPATCHED: "#2563eb",
  RESOLVED: "#16a34a",
};

export function HotspotMap({ hotspots }: { hotspots: HotspotDto[] }) {
  return (
    <MapBase
      legend={
        <>
          <div className="legend-title">Hotspot status</div>
          {Object.entries(STATUS_COLOR).map(([status, color]) => (
            <div key={status} className="legend-row">
              <div className="legend-swatch" style={{ background: color }} />
              <span>{status.toLowerCase()}</span>
            </div>
          ))}
        </>
      }
    >
      {hotspots.map((h) => {
        const color = STATUS_COLOR[h.status];
        return (
          <CircleMarker
            key={h.id}
            center={[h.lat, h.lng]}
            radius={Math.min(28, 10 + h.reportCount * 3)}
            pathOptions={{ color, weight: 1.5, fillColor: color, fillOpacity: 0.45 }}
          >
            <Popup>
              <strong>{h.label ?? h.segmentName ?? "Hotspot"}</strong>
              <br />
              Reports: {h.reportCount}
              <br />
              Cleanliness: {Math.round(h.cleanliness)} / 100
              <br />
              Status: {h.status.toLowerCase()}
            </Popup>
          </CircleMarker>
        );
      })}
    </MapBase>
  );
}
