import "leaflet/dist/leaflet.css";
import type { ReactNode } from "react";
import { MapContainer, ScaleControl, TileLayer, useMapEvents } from "react-leaflet";
import { LEUVEN_BOUNDS, LEUVEN_CENTER, LEUVEN_MAX_ZOOM, LEUVEN_MIN_ZOOM } from "./leuvenMap";

interface Props {
  children?: ReactNode;
  /** Drawn over the bottom-right corner of the map. */
  legend?: ReactNode;
}

/**
 * The Leuven street map every Leuven Go map is drawn on. OpenStreetMap's
 * standard tiles need no API key, only the attribution.
 */
export function MapBase({ children, legend }: Props) {
  return (
    <div className="map-shell">
      <MapContainer
        center={LEUVEN_CENTER}
        zoom={14}
        minZoom={LEUVEN_MIN_ZOOM}
        maxZoom={LEUVEN_MAX_ZOOM}
        maxBounds={LEUVEN_BOUNDS}
        maxBoundsViscosity={0.9}
        scrollWheelZoom={false}
      >
        <TileLayer
          url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          maxZoom={LEUVEN_MAX_ZOOM}
        />
        <ScaleControl position="bottomleft" imperial={false} />
        <WheelZoomOnceClicked />
        {children}
      </MapContainer>
      {legend && <div className="legend">{legend}</div>}
    </div>
  );
}

/**
 * Scrolling the page over a map shouldn't zoom it. The wheel zooms only after
 * the map has been clicked, until the pointer leaves it.
 */
function WheelZoomOnceClicked() {
  const map = useMapEvents({
    click: () => map.scrollWheelZoom.enable(),
    mouseout: () => map.scrollWheelZoom.disable(),
  });
  return null;
}
