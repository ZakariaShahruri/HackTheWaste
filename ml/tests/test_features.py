from datetime import datetime

import pandas as pd
import pytest

from features import FEATURE_NAMES, HOTSPOTS, build_features, haversine_km, leuven_times


def column(rows, name):
    return rows[:, FEATURE_NAMES.index(name)]


def test_one_row_per_point_in_feature_order():
    rows = build_features([50.87, 50.88, 50.89], [4.69, 4.70, 4.71], [datetime(2026, 11, 18, 12)] * 3)
    assert rows.shape == (3, len(FEATURE_NAMES))


def test_a_saturday_night_out():
    rows = build_features([50.88], [4.70], [datetime(2026, 11, 21, 23, 30)])
    assert column(rows, "hour")[0] == 23
    assert column(rows, "dow")[0] == 5
    assert column(rows, "is_weekend")[0] == 1
    assert column(rows, "is_night")[0] == 1
    assert column(rows, "is_rush")[0] == 0


def test_the_wednesday_morning_rush():
    rows = build_features([50.88], [4.70], [datetime(2026, 11, 18, 8)])
    assert column(rows, "is_weekend")[0] == 0
    assert column(rows, "is_night")[0] == 0
    assert column(rows, "is_rush")[0] == 1


def test_distance_to_a_hotspot_is_zero_on_it():
    lat, lng = HOTSPOTS["oude_markt"]
    rows = build_features([lat], [lng], [datetime(2026, 11, 18, 12)])
    assert column(rows, "dist_oude_markt")[0] == pytest.approx(0)
    assert column(rows, "dist_station")[0] > 0


def test_haversine_matches_a_known_distance():
    # One degree of latitude is about 111.2 km anywhere on Earth.
    assert haversine_km(50.0, 4.7, 51.0, 4.7) == pytest.approx(111.2, abs=0.1)


@pytest.mark.parametrize("given, leuven", [
    ("2026-11-21T22:30:00Z", "2026-11-21T23:30:00"),       # winter, UTC+1
    ("2026-07-04T21:00:00+00:00", "2026-07-04T23:00:00"),  # summer, UTC+2
    ("2026-07-04T23:00:00+02:00", "2026-07-04T23:00:00"),
    ("2026-07-04T23:00:00", "2026-07-04T23:00:00"),        # no offset: already Leuven time
])
def test_times_are_read_as_leuven_wall_clock_time(given, leuven):
    assert leuven_times([given])[0] == pd.Timestamp(leuven)


def test_a_late_utc_evening_is_already_saturday_night_in_leuven():
    rows = build_features([50.88], [4.70], ["2026-11-20T23:30:00Z"])
    assert column(rows, "dow")[0] == 5
    assert column(rows, "hour")[0] == 0
