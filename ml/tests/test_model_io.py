import json
import subprocess
import sys
from pathlib import Path

import pytest

import model_io

ML_DIR = Path(__file__).resolve().parent.parent


def test_the_api_starts_from_any_working_directory(tmp_path):
    result = subprocess.run(
        [sys.executable, "-c", "import api"],
        cwd=tmp_path, env={"PYTHONPATH": str(ML_DIR)}, capture_output=True, text=True,
    )
    assert result.returncode == 0, result.stderr


def test_a_model_trained_on_other_features_is_refused(tmp_path, monkeypatch):
    stale = tmp_path / "feature_names.json"
    stale.write_text(json.dumps(["latitude", "longitude"]))
    monkeypatch.setattr(model_io, "FEATURES_FILE", stale)

    with pytest.raises(RuntimeError, match="other features"):
        model_io.load()
