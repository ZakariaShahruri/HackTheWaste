import type { LatLngBoundsExpression, LatLngTuple } from "leaflet";

// Stadhuis van Leuven (city hall, Grote Markt) — exact city centre.
export const LEUVEN_CENTER: LatLngTuple = [50.8794, 4.7009];

// Soft bounding box around greater Leuven (Heverlee, Kessel-Lo, Wijgmaal,
// Wilsele) so users can pan the map but can't drift out to Brussels.
export const LEUVEN_BOUNDS: LatLngBoundsExpression = [
  [50.835, 4.6],
  [50.93, 4.78],
];

export const LEUVEN_MIN_ZOOM = 12;
export const LEUVEN_MAX_ZOOM = 18;

// Where reports are accepted; the same box as the backend's LeuvenArea.
const REPORT_AREA = { south: 50.83, north: 50.93, west: 4.62, east: 4.79 };

export function isInLeuven(lat: number, lng: number): boolean {
  return lat >= REPORT_AREA.south && lat <= REPORT_AREA.north && lng >= REPORT_AREA.west && lng <= REPORT_AREA.east;
}
