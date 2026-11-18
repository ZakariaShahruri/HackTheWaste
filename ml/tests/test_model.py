"""The committed model has learned the patterns in the training data."""

from api import MODEL_METRICS, predict
from features import HOTSPOTS


def likelihood(place, time):
    lat, lng = place
    return predict([lat], [lng], [time])[0]


def test_it_explains_most_of_the_variance_on_held_out_data():
    assert MODEL_METRICS["r2"] > 0.8


def test_oude_markt_is_worst_on_a_thursday_night():
    oude_markt = HOTSPOTS["oude_markt"]
    assert likelihood(oude_markt, "2026-11-19T23:00:00") > likelihood(oude_markt, "2026-11-17T15:00:00") + 0.2


def test_the_station_is_busier_at_noon_than_at_three_in_the_morning():
    station = HOTSPOTS["station"]
    assert likelihood(station, "2026-11-18T12:00:00") > likelihood(station, "2026-11-18T03:00:00") + 0.2


def test_campus_is_quieter_over_the_summer_break():
    campus = HOTSPOTS["ku_leuven"]
    assert likelihood(campus, "2026-08-19T11:00:00") < likelihood(campus, "2026-11-18T11:00:00")


def test_the_outskirts_stay_clean():
    assert likelihood((50.86, 4.67), "2026-11-19T23:00:00") < 0.2
