import { useState } from "react";
import { useApi } from "../../hooks/useApi";
import { MapContainer, TileLayer, ScaleControl } from "react-leaflet";
import "./leafletSetup";
import {
  LEUVEN_BOUNDS,
  LEUVEN_CENTER,
  LEUVEN_MAX_ZOOM,
  LEUVEN_MIN_ZOOM,
} from "./leafletSetup";
import { PredictiveHeatmapLayer } from "./PredictiveHeatmapLayer";
import { api } from "../../api/client";

const DAYS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
const GRID_STEPS = 35;

interface Props {
  height?: number;
}

export function PredictiveMap({ height = 540 }: Props) {
  const today = (new Date().getDay() + 6) % 7;
  const [day, setDay]   = useState(today);
  const [hour, setHour] = useState(new Date().getHours());
  const prediction = useApi(
    (signal) => api.predictGrid(day, hour, GRID_STEPS, signal),
    { key: `${day}:${hour}` },
  );
  const points = prediction.data?.points ?? [];
  const loading = prediction.loading;
  const mlOffline = prediction.error !== null || prediction.data?.available === false;

  const peak = points.length ? Math.max(...points.map((p) => p.likelihood)) : 0;
  const peakPct = Math.round(peak * 100);
  const peakColor =
    peak > 0.65 ? "#dc2626"
    : peak > 0.4 ? "#f97316"
    : peak > 0.2 ? "#eab308"
    : "#22c55e";

  return (
    <div>
      {/* Time scrubber */}
      <div className="predictive-controls">
        <div className="predictive-day-row">
          {DAYS.map((d, i) => (
            <button
              key={d}
              className={`predictive-day-btn${day === i ? " active" : ""}`}
              onClick={() => setDay(i)}
            >
              {d}
            </button>
          ))}
        </div>

        <div className="predictive-hour-row">
          <span className="predictive-hour-label">
            {String(hour).padStart(2, "0")}:00
          </span>
          <input
            type="range"
            min={0}
            max={23}
            step={1}
            value={hour}
            onChange={(e) => setHour(Number(e.target.value))}
            className="predictive-slider"
          />
          <span className="predictive-status">
            {loading ? (
              <span className="predictive-loading">Predicting…</span>
            ) : mlOffline ? (
              <span className="predictive-error">
                Predictions are offline right now
              </span>
            ) : (
              <span>
                Peak: <strong style={{ color: peakColor }}>{peakPct}%</strong>
              </span>
            )}
          </span>
        </div>
      </div>

      {/* Map */}
      <div className="map-shell" style={{ height }}>
        <MapContainer
          center={LEUVEN_CENTER}
          zoom={14}
          minZoom={LEUVEN_MIN_ZOOM}
          maxZoom={LEUVEN_MAX_ZOOM}
          maxBounds={LEUVEN_BOUNDS}
          maxBoundsViscosity={0.9}
          scrollWheelZoom
          style={{ height: "100%", width: "100%" }}
        >
          <TileLayer
            url="https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png"
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> &copy; CARTO'
            subdomains="abcd"
            maxZoom={LEUVEN_MAX_ZOOM}
          />
          <ScaleControl position="bottomleft" imperial={false} />
          {!mlOffline && <PredictiveHeatmapLayer points={points} />}
        </MapContainer>

        <div className="legend">
          <div style={{ fontWeight: 600, marginBottom: 6, fontSize: 12 }}>
            Trash likelihood
          </div>
          {[
            { color: "#dc2626", label: "Very high" },
            { color: "#f97316", label: "High" },
            { color: "#eab308", label: "Moderate" },
            { color: "#84cc16", label: "Low" },
            { color: "#22c55e", label: "Clean" },
          ].map(({ color, label }) => (
            <div key={label} className="legend-row">
              <div className="legend-swatch" style={{ background: color }} />
              <span>{label}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
