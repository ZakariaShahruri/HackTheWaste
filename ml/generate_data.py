"""
Generates data/leuven_trash.csv: synthetic (latitude, longitude, time, likelihood)
samples for training.

There's no real litter data behind this. The likelihood comes from a
hand-written prior about Leuven: each hotspot has a peak and a pattern for
when it gets messy, fading out with distance, on top of a low baseline.
  - nightlife (Oude Markt, Naamsestraat): Thursday to Saturday nights, with
    the mess lingering until the morning sweep
  - daytime (station, shopping streets, Ladeuzeplein): shop and office hours
  - campus: weekdays during term, quiet over the summer break
  - park: afternoons, mostly at the weekend
Noise is added so the model has something to learn rather than copy.
"""

from pathlib import Path

import numpy as np
import pandas as pd

from features import HOTSPOTS, LAT_RANGE, LEUVEN_CENTER, LEUVEN_TZ, LNG_RANGE, haversine_km

DATA_FILE = Path(__file__).resolve().parent / "data" / "leuven_trash.csv"

BASELINE = 0.10
NOISE = 0.05
N_SAMPLES = 20_000
SEED = 42
START = pd.Timestamp("2025-01-01", tz="UTC")
END = pd.Timestamp("2026-01-01", tz="UTC")


def nightlife(hour, dow, month):
    # Until the morning sweep, the streets still show the night before.
    night_of = np.where(hour <= 9, (dow - 1) % 7, dow)
    big_night = np.where(np.isin(night_of, [3, 4, 5]), 1.0, 0.5)  # Thursday to Saturday
    out = (hour >= 22) | (hour <= 3)
    morning_after = (hour >= 4) & (hour <= 9)
    return np.select([out, morning_after], [big_night, 0.6 * big_night], 0.2)


def daytime(hour, dow, month):
    open_hours = (hour >= 8) & (hour <= 19)
    return np.where(open_hours, np.where(dow >= 5, 0.7, 1.0), 0.15)


def campus(hour, dow, month):
    lectures = (dow < 5) & (hour >= 9) & (hour <= 18)
    summer_break = np.isin(month, [7, 8])
    return np.where(lectures, 1.0, 0.15) * np.where(summer_break, 0.4, 1.0)


def park(hour, dow, month):
    afternoon = (hour >= 12) & (hour <= 20)
    return np.where(afternoon, np.where(dow >= 5, 1.0, 0.5), 0.1)


# Per hotspot: (peak likelihood, radius in km, when it's busy).
PRIOR = {
    "oude_markt":      (0.85, 0.25, nightlife),
    "naamsestraat":    (0.60, 0.30, nightlife),
    "station":         (0.55, 0.30, daytime),
    "ladeuzeplein":    (0.50, 0.20, daytime),
    "bondgenotenlaan": (0.45, 0.30, daytime),
    "diestsestraat":   (0.40, 0.30, daytime),
    "ku_leuven":       (0.45, 0.35, campus),
    "stadspark":       (0.35, 0.25, park),
}


def prior_likelihood(lats, lngs, local_times: pd.DatetimeIndex) -> np.ndarray:
    hour = local_times.hour.to_numpy()
    dow = local_times.day_of_week.to_numpy()
    month = local_times.month.to_numpy()

    likelihood = np.full(len(lats), BASELINE)
    for name, (peak, radius_km, pattern) in PRIOR.items():
        nearness = np.clip(1 - haversine_km(lats, lngs, *HOTSPOTS[name]) / radius_km, 0, None)
        likelihood += peak * pattern(hour, dow, month) * nearness
    return likelihood


def sample_points(n: int, rng: np.random.Generator):
    """Most samples close to the hotspots, where the prior changes fastest; the rest anywhere in town."""
    near = int(n * 0.6)
    centres = np.array(list(HOTSPOTS.values()))[rng.integers(0, len(HOTSPOTS), near)]
    lats = np.concatenate([centres[:, 0] + rng.normal(0, 0.0012, near),
                           rng.normal(LEUVEN_CENTER[0], 0.013, n - near)])
    lngs = np.concatenate([centres[:, 1] + rng.normal(0, 0.0018, near),
                           rng.normal(LEUVEN_CENTER[1], 0.020, n - near)])
    return np.clip(lats, *LAT_RANGE), np.clip(lngs, *LNG_RANGE)


def generate(n: int, rng: np.random.Generator) -> pd.DataFrame:
    lats, lngs = sample_points(n, rng)
    span = int((END - START).total_seconds())
    times = (START + pd.to_timedelta(rng.integers(0, span, n), unit="s")).tz_convert(LEUVEN_TZ)

    likelihood = prior_likelihood(lats, lngs, times) + rng.normal(0, NOISE, n)
    return pd.DataFrame({
        "latitude": lats.round(6),
        "longitude": lngs.round(6),
        "time": times.strftime("%Y-%m-%dT%H:%M:%S%:z"),
        "likelihood": np.clip(likelihood, 0.0, 1.0).round(4),
    })


def main():
    df = generate(N_SAMPLES, np.random.default_rng(SEED))
    DATA_FILE.parent.mkdir(exist_ok=True)
    df.to_csv(DATA_FILE, index=False)
    print(f"Wrote {len(df)} rows → {DATA_FILE}")
    print(df.describe())


if __name__ == "__main__":
    main()
