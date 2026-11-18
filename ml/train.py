"""
Trains a Random Forest regressor on the CSV in data/ and saves it to model/.

The features (place, time of day and week, distance to known hotspots) come
from features.py, which the API uses too.

Output:
  model/rf_trash.joblib    – trained RandomForestRegressor
  model/feature_names.json – the feature order it was trained on
  model/metrics.json       – R², MAE on a held-out test set
"""

from pathlib import Path

import pandas as pd
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_absolute_error, r2_score
from sklearn.model_selection import train_test_split

import model_io
from features import FEATURE_NAMES, build_features

DATA_DIR = Path(__file__).resolve().parent / "data"


def main():
    for name in ["leuven_trash_likelihood.csv", "leuven_trash.csv"]:
        if (DATA_DIR / name).exists():
            csv_path = DATA_DIR / name
            break
    else:
        raise FileNotFoundError(f"No training CSV in {DATA_DIR}, run generate_data.py first")

    df = pd.read_csv(csv_path)
    df.columns = [c.lower() for c in df.columns]
    print(f"Loaded {len(df)} rows from {csv_path}")

    times = pd.to_datetime(df["time"], utc=True).dt.tz_convert("Europe/Brussels")
    X = build_features(df["latitude"], df["longitude"], times)
    y = df["likelihood"].values

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42
    )

    print("Training Random Forest …")
    rf = RandomForestRegressor(
        n_estimators=200,
        max_depth=12,
        min_samples_leaf=4,
        n_jobs=-1,
        random_state=42,
    )
    rf.fit(X_train, y_train)

    y_pred = rf.predict(X_test)
    r2  = r2_score(y_test, y_pred)
    mae = mean_absolute_error(y_test, y_pred)
    print(f"Test R²={r2:.4f}  MAE={mae:.4f}")

    model_io.save(rf, {"r2": round(r2, 4), "mae": round(mae, 4), "n_train": len(X_train)})
    print(f"Saved model → {model_io.MODEL_FILE}")

    importances = sorted(
        zip(FEATURE_NAMES, rf.feature_importances_), key=lambda x: -x[1]
    )
    print("\nTop-10 feature importances:")
    for feat, imp in importances[:10]:
        print(f"  {feat:<22} {imp:.4f}")


if __name__ == "__main__":
    main()
