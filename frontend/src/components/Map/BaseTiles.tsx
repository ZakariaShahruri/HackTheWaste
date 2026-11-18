import { TileLayer } from "react-leaflet";
import { LEUVEN_MAX_ZOOM } from "./leafletSetup";

/**
 * The street map under every Leuven Go map. OpenStreetMap's standard tiles
 * need no API key (CARTO's now do), only this attribution.
 */
export function BaseTiles() {
  return (
    <TileLayer
      url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
      attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
      maxZoom={LEUVEN_MAX_ZOOM}
    />
  );
}
