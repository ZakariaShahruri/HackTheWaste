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
