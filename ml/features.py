"""
Turns a place and a time into the model's features. Training and serving both
go through build_features, so the two can't drift apart.
"""

import numpy as np
import pandas as pd

LEUVEN_TZ = "Europe/Brussels"
LEUVEN_CENTER = (50.8798, 4.7005)

# The area the training data covers; anything outside it would be a guess.
LAT_RANGE = (50.85, 50.91)
LNG_RANGE = (4.66, 4.75)

# The same spots the backend's demo city uses, so the heatmap lines up with it.
HOTSPOTS = {
    "oude_markt":      (50.8785, 4.7000),
    "station":         (50.8812, 4.7150),
    "bondgenotenlaan": (50.8803, 4.7090),
    "ku_leuven":       (50.8775, 4.7019),  # Hogeschoolplein
    "stadspark":       (50.8757, 4.7023),  # Sint-Donatuspark
    "naamsestraat":    (50.8752, 4.7008),
    "diestsestraat":   (50.8830, 4.7090),
    "ladeuzeplein":    (50.8777, 4.7040),
}

FEATURE_NAMES = (
    ["latitude", "longitude", "hour", "dow", "month",
     "is_weekend", "is_night", "is_rush", "dist_center"]
    + [f"dist_{name}" for name in HOTSPOTS]
)


def haversine_km(lat1, lng1, lat2, lng2):
    dlat = np.radians(lat2 - lat1)
    dlng = np.radians(lng2 - lng1)
    a = (np.sin(dlat / 2) ** 2
         + np.cos(np.radians(lat1)) * np.cos(np.radians(lat2)) * np.sin(dlng / 2) ** 2)
    return 6371.0 * 2 * np.arcsin(np.sqrt(np.clip(a, 0, 1)))


def leuven_times(times) -> pd.DatetimeIndex:
    """
    Leuven wall-clock times, which is what the hour and weekday features mean.
    Times with an offset are converted; times without one are taken to be
    Leuven time already.
    """
    stamps = (pd.Timestamp(t) for t in times)
    return pd.DatetimeIndex([t.tz_convert(LEUVEN_TZ).tz_localize(None) if t.tzinfo else t for t in stamps])


def build_features(lats, lngs, times) -> np.ndarray:
    """One row per point, columns in FEATURE_NAMES order. `times` may be strings or datetimes."""
    lats = np.asarray(lats, dtype=float)
    lngs = np.asarray(lngs, dtype=float)
    times = leuven_times(times)
    hour = times.hour.to_numpy()
    dow = times.day_of_week.to_numpy()  # 0 = Monday

    columns = [
        lats,
        lngs,
        hour,
        dow,
        times.month.to_numpy(),
        dow >= 5,
        (hour >= 22) | (hour <= 3),
        ((hour >= 7) & (hour <= 9)) | ((hour >= 17) & (hour <= 19)),
        haversine_km(lats, lngs, *LEUVEN_CENTER),
    ] + [haversine_km(lats, lngs, hlat, hlng) for hlat, hlng in HOTSPOTS.values()]
    return np.column_stack(columns).astype(float)
