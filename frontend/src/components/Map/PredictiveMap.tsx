import { useState } from "react";
import { api } from "../../api/client";
import { useApi } from "../../hooks/useApi";
import { useDebounced } from "../../hooks/useDebounced";
import { MapBase } from "./MapBase";
import { PredictiveHeatmapLayer } from "./PredictiveHeatmapLayer";
import { LIKELIHOOD_BANDS, cssColor, likelihoodColor } from "../../lib/heatmap";

const DAYS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
// ~110 m cells over the forecast area; the backend allows up to 60.
const GRID_STEPS = 50;
// Dragging the hour slider shouldn't fire a forecast for every hour it passes.
const SCRUB_DEBOUNCE_MS = 250;

export function PredictiveMap() {
  const now = new Date();
  const [day, setDay] = useState((now.getDay() + 6) % 7);
  const [hour, setHour] = useState(now.getHours());
  const selected = `${day}:${hour}`;
  const query = useDebounced(selected, SCRUB_DEBOUNCE_MS);

  const prediction = useApi(
    (signal) => {
      const [qDay, qHour] = query.split(":").map(Number);
      return api.predictGrid(qDay, qHour, GRID_STEPS, signal);
    },
    { key: query },
  );
  const points = prediction.data?.points ?? [];
  const offline = prediction.error !== null || prediction.data?.available === false;
  const pending = prediction.loading || query !== selected;
  const peak = points.length ? Math.max(...points.map((p) => p.likelihood)) : 0;

  return (
    <div>
      <div className="predictive-controls">
        <div className="predictive-day-row" role="group" aria-label="Day of the week">
          {DAYS.map((d, i) => (
            <button
              key={d}
              type="button"
              className={`predictive-day-btn${day === i ? " active" : ""}`}
              aria-pressed={day === i}
              onClick={() => setDay(i)}
            >
              {d}
            </button>
          ))}
        </div>

        <div className="predictive-hour-row">
          <label htmlFor="forecast-hour" className="predictive-hour-label">
            {String(hour).padStart(2, "0")}:00
          </label>
          <input
            id="forecast-hour"
            type="range"
            min={0}
            max={23}
            step={1}
            value={hour}
            onChange={(e) => setHour(Number(e.target.value))}
            className="predictive-slider"
            aria-label="Hour of the day"
          />
          <span className="predictive-status" aria-live="polite">
            {offline ? (
              <span className="predictive-offline">Forecast unavailable right now</span>
            ) : pending ? (
              <span className="predictive-loading">Predicting…</span>
            ) : (
              <span>
                Peak: <strong style={{ color: likelihoodColor(peak) }}>{Math.round(peak * 100)}%</strong>
              </span>
            )}
          </span>
        </div>
      </div>

      <MapBase
        legend={
          <>
            <div className="legend-title">Trash likelihood</div>
            {LIKELIHOOD_BANDS.map(({ rgb, label }) => (
              <div key={label} className="legend-row">
                <div className="legend-swatch" style={{ background: cssColor(rgb) }} />
                <span>{label}</span>
              </div>
            ))}
          </>
        }
      >
        {!offline && <PredictiveHeatmapLayer points={points} />}
      </MapBase>
    </div>
  );
}
