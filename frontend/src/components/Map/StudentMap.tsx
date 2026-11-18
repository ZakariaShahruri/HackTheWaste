import { useMemo } from "react";
import { CircleMarker, Polygon, Popup, Tooltip } from "react-leaflet";
import type { LatLngTuple } from "leaflet";
import type { FacultyDto, HotspotDto, StreetSegmentDto } from "../../api/types";
import { SCORE_BANDS, UNSCORED_COLOR, scoreColor } from "../../lib/scores";
import { MapBase } from "./MapBase";

interface Props {
  faculties: FacultyDto[];
  segments: StreetSegmentDto[];
  hotspots: HotspotDto[];
  selectedFaculty?: string;
}

/** The outer ring of a GeoJSON polygon, flipped to Leaflet's [lat, lng]. */
function polygonToLatLng(geo: string): LatLngTuple[] | null {
  try {
    const ring = (JSON.parse(geo) as { coordinates?: number[][][] }).coordinates?.[0];
    return ring && ring.length >= 3 ? ring.map(([lng, lat]) => [lat, lng]) : null;
  } catch {
    return null;
  }
}

export function StudentMap({ faculties, segments, hotspots, selectedFaculty }: Props) {
  const territories = useMemo(
    () =>
      faculties.flatMap((faculty) => {
        const coords = faculty.territoryGeoJson ? polygonToLatLng(faculty.territoryGeoJson) : null;
        return coords ? [{ faculty, coords }] : [];
      }),
    [faculties],
  );

  return (
    <MapBase
      legend={
        <>
          <div className="legend-title">Cleanliness</div>
          {SCORE_BANDS.map((band) => (
            <div key={band.min} className="legend-row">
              <div className="legend-swatch" style={{ background: band.color }} />
              {band.label}
            </div>
          ))}
          <div className="legend-row">
            <div className="legend-swatch" style={{ background: UNSCORED_COLOR }} />
            not scored yet
          </div>
        </>
      }
    >
      {territories.map(({ faculty, coords }) => {
        const dimmed = selectedFaculty && selectedFaculty !== faculty.shortCode;
        return (
          <Polygon
            key={faculty.id}
            positions={coords}
            pathOptions={{
              color: faculty.color,
              fillColor: faculty.color,
              fillOpacity: dimmed ? 0.06 : 0.18,
              weight: dimmed ? 1 : 2,
            }}
          >
            <Tooltip sticky>
              <strong>
                {faculty.emoji} {faculty.name}
              </strong>
              <br />
              {faculty.points} pts · {faculty.members} students
            </Tooltip>
          </Polygon>
        );
      })}

      {segments.map((s) => (
        <CircleMarker
          key={s.id}
          center={[s.lat, s.lng]}
          radius={10}
          pathOptions={{ color: "white", weight: 2, fillColor: scoreColor(s.aiCleanlinessScore), fillOpacity: 0.95 }}
        >
          <Popup>
            <strong>{s.name}</strong>
            <br />
            Score: {s.aiCleanlinessScore === null ? "not scored yet" : `${s.aiCleanlinessScore.toFixed(1)} / 100`}
            <br />
            Held by: {s.facultyName ?? "—"}
            <br />
            Reports (recent): {s.recentReportCount}
          </Popup>
        </CircleMarker>
      ))}

      {hotspots
        .filter((h) => h.status !== "RESOLVED")
        .map((h) => (
          <CircleMarker
            key={`h${h.id}`}
            center={[h.lat, h.lng]}
            radius={Math.min(20, 6 + h.reportCount * 2)}
            pathOptions={{ color: "#7c2d12", weight: 1, dashArray: "4 3", fillColor: "#fb923c", fillOpacity: 0.5 }}
          >
            <Popup>
              <strong>🔥 {h.label}</strong>
              <br />
              {h.reportCount} reports merged
              <br />
              Cleanliness {Math.round(h.cleanliness)} / 100
            </Popup>
          </CircleMarker>
        ))}
    </MapBase>
  );
}
