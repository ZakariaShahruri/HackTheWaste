from fastapi.testclient import TestClient

from api import app

client = TestClient(app)

NOON = "2026-11-18T12:00:00"


def test_health_reports_model_metrics():
    body = client.get("/health").json()
    assert body["status"] == "ok"
    assert "r2" in body["model_metrics"]


def test_predict_returns_a_likelihood_between_0_and_1():
    resp = client.post("/predict", json={"latitude": 50.8793, "longitude": 4.7005, "time": NOON})
    assert resp.status_code == 200
    assert 0.0 <= resp.json()["likelihood"] <= 1.0


def test_grid_has_one_point_per_cell():
    resp = client.post("/predict/grid", json={"time": NOON, "lat_steps": 5, "lng_steps": 6})
    assert resp.status_code == 200
    body = resp.json()
    assert body["count"] == 30
    assert len(body["points"]) == 30
    assert all(0.0 <= p["likelihood"] <= 1.0 for p in body["points"])


def test_invalid_time_is_a_400():
    resp = client.post("/predict", json={"latitude": 50.88, "longitude": 4.70, "time": "tomorrow"})
    assert resp.status_code == 400


def test_points_outside_leuven_are_rejected():
    resp = client.post("/predict", json={"latitude": 52.37, "longitude": 4.90, "time": NOON})
    assert resp.status_code == 422


def test_browsers_are_not_allowed_to_call_the_service_directly():
    resp = client.post(
        "/predict",
        json={"latitude": 50.88, "longitude": 4.70, "time": NOON},
        headers={"Origin": "https://leuvengo.example"},
    )
    assert "access-control-allow-origin" not in resp.headers


def test_a_batch_scores_each_point_like_a_single_prediction():
    points = [
        {"latitude": 50.8793, "longitude": 4.7005, "time": "2026-11-21T23:00:00"},
        {"latitude": 50.8700, "longitude": 4.6900, "time": "2026-11-18T08:30:00"},
    ]
    batch = client.post("/predict/batch", json={"points": points}).json()
    singles = [client.post("/predict", json=p).json() for p in points]
    assert batch == singles


def test_an_empty_batch_is_an_empty_list():
    assert client.post("/predict/batch", json={"points": []}).json() == []


def test_a_utc_time_is_scored_as_the_same_moment_in_leuven():
    def likelihood(time):
        return client.post("/predict", json={"latitude": 50.8793, "longitude": 4.7005, "time": time}).json()["likelihood"]

    assert likelihood("2026-11-21T22:00:00Z") == likelihood("2026-11-21T23:00:00")
    assert likelihood("2026-11-21T22:00:00Z") != likelihood("2026-11-21T22:00:00")
