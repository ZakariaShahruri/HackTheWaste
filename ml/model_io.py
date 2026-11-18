"""Saves and loads the trained model next to this file, wherever it's run from."""

import json
from pathlib import Path

import joblib

from features import FEATURE_NAMES

MODEL_DIR = Path(__file__).resolve().parent / "model"
MODEL_FILE = MODEL_DIR / "litter_model.joblib"
FEATURES_FILE = MODEL_DIR / "feature_names.json"
METRICS_FILE = MODEL_DIR / "metrics.json"


def save(model, metrics: dict) -> None:
    MODEL_DIR.mkdir(exist_ok=True)
    joblib.dump(model, MODEL_FILE, compress=3)
    FEATURES_FILE.write_text(json.dumps(FEATURE_NAMES, indent=2) + "\n")
    METRICS_FILE.write_text(json.dumps(metrics, indent=2) + "\n")


def load():
    """The model and its metrics. Refuses a model trained on different features."""
    if not MODEL_FILE.exists():
        raise RuntimeError(f"No model at {MODEL_FILE}, run train.py first")
    if json.loads(FEATURES_FILE.read_text()) != FEATURE_NAMES:
        raise RuntimeError("The saved model uses other features than features.py, run train.py again")
    return joblib.load(MODEL_FILE), json.loads(METRICS_FILE.read_text())
