import { useState, type FormEvent } from "react";
import { Link } from "react-router";
import type { LatLngTuple } from "leaflet";
import { api } from "../../api/client";
import type { DemoReportResult } from "../../api/types";
import { useAuth } from "../../context/auth";
import { useApi } from "../../hooks/useApi";
import { useLocalPhoto } from "../../hooks/useLocalPhoto";
import { shrinkPhoto } from "../../lib/photos";
import { reportErrorMessage } from "../../lib/reportErrors";
import { LocationPicker } from "./LocationPicker";
import { VerdictCard } from "./VerdictCard";

interface Filed {
  result: DemoReportResult;
  earnedPoints?: number;
}

/**
 * "Try it in your browser": the same pipeline as a WhatsApp report (location,
 * photo, AI check, hotspot), for visitors who don't want to set up WhatsApp.
 */
export function BrowserReport() {
  const { user, setPoints } = useAuth();
  const faculties = useApi((signal) => api.faculties(signal));
  const [spot, setSpot] = useState<LatLngTuple | null>(null);
  const [photo, setPhoto] = useLocalPhoto();
  const [clan, setClan] = useState(user?.facultyShortCode ?? "");
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [filed, setFiled] = useState<Filed | null>(null);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!spot || !photo) return;
    setSending(true);
    setError(null);
    try {
      const result = await api.submitDemoReport({
        photo: await shrinkPhoto(photo.file),
        lat: spot[0],
        lng: spot[1],
        clan: clan || undefined,
      });
      setFiled({ result, earnedPoints: await refreshPoints() });
    } catch (err) {
      setError(reportErrorMessage(err));
    } finally {
      setSending(false);
    }
  };

  /** A logged-in reporter's new balance, and what this report added to it. */
  const refreshPoints = async (): Promise<number | undefined> => {
    if (!user) return undefined;
    try {
      const fresh = await api.me();
      setPoints(fresh.points);
      return fresh.points - user.points;
    } catch {
      return undefined;
    }
  };

  const reset = () => {
    setFiled(null);
    setPhoto(null);
    setSpot(null);
  };

  if (filed) return <VerdictCard result={filed.result} earnedPoints={filed.earnedPoints} onAnother={reset} />;

  return (
    <form className="browser-report" onSubmit={(e) => void submit(e)}>
      <ol className="report-steps">
        <li>
          <h2 className="report-step-title">Where is it?</h2>
          <LocationPicker value={spot} onChange={setSpot} />
        </li>

        <li>
          <h2 className="report-step-title">Snap the litter</h2>
          <label className={`photo-drop${photo ? " has-photo" : ""}`}>
            <input
              type="file"
              name="photo"
              accept="image/*"
              capture="environment"
              onChange={(e) => setPhoto(e.target.files?.[0] ?? null)}
            />
            {photo ? (
              <img src={photo.url} alt="The photo you picked" />
            ) : (
              <span>📸 Take a photo or choose one</span>
            )}
          </label>
        </li>

        <li>
          <h2 className="report-step-title">
            Score for a clan <span className="muted">(optional)</span>
          </h2>
          <select name="clan" aria-label="Clan" className="clan-select" value={clan} onChange={(e) => setClan(e.target.value)}>
            <option value="">No clan</option>
            {(faculties.data ?? []).map((f) => (
              <option key={f.id} value={f.shortCode}>
                {f.emoji} {f.name === f.shortCode ? f.name : `${f.name} (${f.shortCode})`}
              </option>
            ))}
          </select>
          {!user && (
            <p className="muted report-hint">
              <Link to="/login" state={{ from: { pathname: "/report" } }}>Log in</Link> to earn points you can spend on
              rewards.
            </p>
          )}
        </li>
      </ol>

      {error && (
        <div role="alert" className="form-error">
          {error}
        </div>
      )}

      <button type="submit" className="btn btn-primary report-submit" disabled={!spot || !photo || sending}>
        {sending ? "Checking your photo…" : "Send report"}
      </button>
      {(!spot || !photo) && (
        <p className="muted report-hint">{!spot ? "Pick a spot on the map first." : "Add a photo to send the report."}</p>
      )}
    </form>
  );
}
