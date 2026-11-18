import { useEffect, useState } from "react";
import { CircleMarker, useMap, useMapEvents } from "react-leaflet";
import type { LatLngTuple } from "leaflet";
import { MapBase } from "../Map/MapBase";
import { isInLeuven } from "../Map/leuvenMap";

interface Props {
  value: LatLngTuple | null;
  onChange: (spot: LatLngTuple) => void;
}

const OUTSIDE = "That's outside Leuven. Leuven Go only covers the city, so pick a spot on the map.";

/** Where the litter is: the device's location, or a tap on the map. */
export function LocationPicker({ value, onChange }: Props) {
  const [note, setNote] = useState<string | null>(null);

  const pick = (lat: number, lng: number) => {
    if (!isInLeuven(lat, lng)) {
      setNote(OUTSIDE);
      return;
    }
    setNote(null);
    onChange([lat, lng]);
  };

  const locate = () => {
    if (!("geolocation" in navigator)) {
      setNote("This browser can't share its location. Tap the map instead.");
      return;
    }
    setNote("Finding you…");
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => pick(coords.latitude, coords.longitude),
      () => setNote("Couldn't get your location. Tap the map instead."),
      { enableHighAccuracy: true, timeout: 10_000 },
    );
  };

  return (
    <div className="location-picker">
      <div className="row location-actions">
        <button type="button" className="btn btn-ghost btn-sm" onClick={locate}>
          📍 Use my location
        </button>
        <span className="muted">or tap the map where the litter is</span>
      </div>
      {note && (
        <p role="status" className="location-note">
          {note}
        </p>
      )}
      <MapBase>
        <PickOnClick onPick={pick} />
        {value && (
          <>
            <ShowSpot spot={value} />
            <CircleMarker
              center={value}
              radius={11}
              pathOptions={{ color: "white", weight: 3, fillColor: "#0f766e", fillOpacity: 1 }}
            />
          </>
        )}
      </MapBase>
    </div>
  );
}

function PickOnClick({ onPick }: { onPick: (lat: number, lng: number) => void }) {
  useMapEvents({ click: (e) => onPick(e.latlng.lat, e.latlng.lng) });
  return null;
}

/** Brings a located spot into view; a spot tapped on the map already is. */
function ShowSpot({ spot }: { spot: LatLngTuple }) {
  const map = useMap();
  useEffect(() => {
    if (!map.getBounds().contains(spot)) map.setView(spot, Math.max(map.getZoom(), 16));
  }, [map, spot]);
  return null;
}
