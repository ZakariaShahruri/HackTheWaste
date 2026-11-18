import numpy as np
import pandas as pd

from features import HOTSPOTS, LAT_RANGE, LNG_RANGE, leuven_times
from generate_data import BASELINE, generate, prior_likelihood

OUDE_MARKT = HOTSPOTS["oude_markt"]
CAMPUS = HOTSPOTS["ku_leuven"]


def prior(place, time):
    lat, lng = place
    return prior_likelihood(np.array([lat]), np.array([lng]), leuven_times([time]))[0]


def test_nightlife_peaks_on_a_thursday_night_and_lingers_until_morning():
    thursday_night = prior(OUDE_MARKT, "2026-11-19T23:00:00")
    friday_morning = prior(OUDE_MARKT, "2026-11-20T07:00:00")
    tuesday_afternoon = prior(OUDE_MARKT, "2026-11-17T15:00:00")
    assert thursday_night > friday_morning > tuesday_afternoon


def test_after_midnight_still_counts_as_the_night_before():
    assert prior(OUDE_MARKT, "2026-11-21T01:00:00") == prior(OUDE_MARKT, "2026-11-20T23:00:00")


def test_campus_is_quieter_over_the_summer_break():
    assert prior(CAMPUS, "2026-08-19T11:00:00") < prior(CAMPUS, "2026-11-18T11:00:00")


def test_far_from_every_hotspot_only_the_baseline_is_left():
    assert prior((50.86, 4.67), "2026-11-19T23:00:00") == BASELINE


def test_samples_stay_in_the_training_area_with_leuven_times():
    df = generate(500, np.random.default_rng(1))
    assert df["latitude"].between(*LAT_RANGE).all()
    assert df["longitude"].between(*LNG_RANGE).all()
    assert df["likelihood"].between(0, 1).all()
    assert df["time"].str.match(r"^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d\+0[12]:00$").all()


def test_the_same_seed_gives_the_same_data():
    pd.testing.assert_frame_equal(generate(200, np.random.default_rng(7)), generate(200, np.random.default_rng(7)))
