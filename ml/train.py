"""
Trains a gradient-boosted tree model on data/leuven_trash.csv and saves it to model/.

The features (place, time of day and week, distance to known hotspots) come
from features.py, which the API uses too. Boosting picks up the
place-and-time interactions (Oude Markt on a Thursday night) that a random
forest of the same size smoothed away.

Output:
  model/litter_model.joblib – trained HistGradientBoostingRegressor
  model/feature_names.json  – the feature order it was trained on
  model/metrics.json        – R², MAE on a held-out test set
"""

import pandas as pd
from sklearn.ensemble import HistGradientBoostingRegressor
from sklearn.inspection import permutation_importance
from sklearn.metrics import mean_absolute_error, r2_score
from sklearn.model_selection import train_test_split

import model_io
from features import FEATURE_NAMES, build_features
from generate_data import DATA_FILE


def main():
    if not DATA_FILE.exists():
        raise FileNotFoundError(f"No training data at {DATA_FILE}, run generate_data.py first")

    df = pd.read_csv(DATA_FILE)
    print(f"Loaded {len(df)} rows from {DATA_FILE}")

    X = build_features(df["latitude"], df["longitude"], df["time"])
    y = df["likelihood"].values

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42
    )

    print("Training gradient-boosted trees …")
    model = HistGradientBoostingRegressor(max_iter=500, random_state=42)
    model.fit(X_train, y_train)

    y_pred = model.predict(X_test)
    r2  = r2_score(y_test, y_pred)
    mae = mean_absolute_error(y_test, y_pred)
    print(f"Test R²={r2:.4f}  MAE={mae:.4f}")

    model_io.save(model, {"r2": round(r2, 4), "mae": round(mae, 4), "n_train": len(X_train)})
    print(f"Saved model → {model_io.MODEL_FILE}")

    importances = permutation_importance(model, X_test, y_test, n_repeats=5, random_state=42)
    ranked = sorted(zip(FEATURE_NAMES, importances.importances_mean), key=lambda x: -x[1])
    print("\nTop-10 feature importances (drop in R² when shuffled):")
    for feat, imp in ranked[:10]:
        print(f"  {feat:<22} {imp:.4f}")


if __name__ == "__main__":
    main()
