"""
FastAPI prediction server.

Endpoints:
  GET  /health              → {"status": "ok", "model_metrics": {...}}
  POST /predict             → single prediction
  POST /predict/batch       → many points in one call
  POST /predict/grid        → a regular grid over Leuven at one time, for the heatmap

Run:
  uvicorn api:app --host 0.0.0.0 --port 8000 --reload
"""

from datetime import datetime

import numpy as np
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field, model_validator

import model_io
from features import LAT_RANGE, LNG_RANGE, build_features

rf, MODEL_METRICS = model_io.load()

MAX_BATCH = 5_000


def predict(lats, lngs, times) -> list[float]:
    scores = np.clip(rf.predict(build_features(lats, lngs, times)), 0.0, 1.0)
    return [round(float(s), 4) for s in scores]


def parse_time(value: str) -> datetime:
    try:
        return datetime.fromisoformat(value)
    except ValueError:
        raise HTTPException(400, f"Invalid ISO-8601 time: {value}")


# No CORS on purpose: only the backend calls this service, browsers never do.
app = FastAPI(title="Leuven Trash Likelihood API", version="1.0.0")


def latitude(default=...):
    return Field(default, ge=LAT_RANGE[0], le=LAT_RANGE[1])


def longitude(default=...):
    return Field(default, ge=LNG_RANGE[0], le=LNG_RANGE[1])


class PredictRequest(BaseModel):
    latitude:  float = latitude()
    longitude: float = longitude()
    time: str = Field(
        ..., description="ISO-8601 timestamp, e.g. 2026-05-09T22:00:00"
    )


class PredictResponse(BaseModel):
    latitude:   float
    longitude:  float
    time:       str
    likelihood: float


class BatchRequest(BaseModel):
    points: list[PredictRequest]


class GridRequest(BaseModel):
    time: str = Field(..., description="ISO-8601 timestamp")
    lat_min:  float = latitude(50.855)
    lat_max:  float = latitude(50.905)
    lng_min:  float = longitude(4.670)
    lng_max:  float = longitude(4.730)
    lat_steps: int  = Field(30, ge=5, le=100)
    lng_steps: int  = Field(30, ge=5, le=100)

    @model_validator(mode="after")
    def min_below_max(self):
        if self.lat_min >= self.lat_max or self.lng_min >= self.lng_max:
            raise ValueError("lat_min and lng_min must be below lat_max and lng_max")
        return self


@app.get("/health")
def health():
    return {"status": "ok", "model_metrics": MODEL_METRICS}


@app.post("/predict", response_model=PredictResponse)
def predict_one(req: PredictRequest):
    return predict_batch(BatchRequest(points=[req]))[0]


@app.post("/predict/batch", response_model=list[PredictResponse])
def predict_batch(req: BatchRequest):
    if len(req.points) > MAX_BATCH:
        raise HTTPException(400, f"Max {MAX_BATCH} points per batch")
    if not req.points:
        return []
    scores = predict(
        [p.latitude for p in req.points],
        [p.longitude for p in req.points],
        [parse_time(p.time) for p in req.points],
    )
    return [
        PredictResponse(latitude=p.latitude, longitude=p.longitude, time=p.time, likelihood=score)
        for p, score in zip(req.points, scores)
    ]


@app.post("/predict/grid")
def predict_grid(req: GridRequest):
    """A flat list of {lat, lng, likelihood}, row by row from lat_min."""
    ts = parse_time(req.time)
    lats, lngs = np.meshgrid(
        np.linspace(req.lat_min, req.lat_max, req.lat_steps),
        np.linspace(req.lng_min, req.lng_max, req.lng_steps),
        indexing="ij",
    )
    lats, lngs = lats.ravel(), lngs.ravel()
    scores = predict(lats, lngs, [ts] * len(lats))
    points = [
        {"lat": round(float(lat), 6), "lng": round(float(lng), 6), "likelihood": score}
        for lat, lng, score in zip(lats, lngs, scores)
    ]
    return {"time": req.time, "count": len(points), "points": points}
