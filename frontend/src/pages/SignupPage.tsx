import { useState } from "react";
import { Link, useNavigate } from "react-router";
import { useAuth } from "../context/auth";
import { errorMessage, isApiError } from "../api/http";
import { useApi } from "../hooks/useApi";
import { api } from "../api/client";
import { AuthCard } from "../components/AuthCard";
import { ClanPills } from "../components/ClanPills";

export default function SignupPage() {
  const { register, loading } = useAuth();
  const navigate = useNavigate();
  const { data: faculties } = useApi((signal) => api.faculties(signal));

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [clan, setClan] = useState<string | undefined>(undefined);
  const [consent, setConsent] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    if (password !== confirm) {
      setError("Passwords do not match");
      return;
    }
    try {
      await register(email, password, clan);
      navigate("/student", { replace: true });
    } catch (err) {
      setError(isApiError(err, 409) ? "Email is already registered" : errorMessage(err, "Sign up failed"));
    }
  }

  return (
    <AuthCard
      title="Create account"
      subtitle="Join Leuven Go and start earning points for your clan"
      wide
      footer={
        <>
          Already have an account? <Link to="/login">Log in</Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="form-grid">
        <div>
          <label htmlFor="email">Email</label>
          <input
            id="email"
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            placeholder="you@example.com"
          />
        </div>

        <div>
          <label htmlFor="password">Password</label>
          <input
            id="password"
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={8}
            maxLength={72}
            placeholder="At least 8 characters"
          />
        </div>

        <div>
          <label htmlFor="confirm">Confirm password</label>
          <input
            id="confirm"
            type="password"
            autoComplete="new-password"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            required
            placeholder="Repeat your password"
          />
        </div>

        <div>
          <div className="field-label">
            Clan <span className="muted">(optional, the faculty your reports score for)</span>
          </div>
          <ClanPills label="Clan" faculties={faculties ?? []} selected={clan} onSelect={setClan} />
          {clan && (
            <p className="field-hint">
              Selected: <strong>{faculties?.find((f) => f.shortCode === clan)?.name}</strong>
            </p>
          )}
        </div>

        <fieldset className="consent">
          <legend className="consent-title">Data consent</legend>
          <ul className="consent-list">
            <li>Your <strong>email and clan</strong> are stored to personalise your experience and credit your reports to the right team.</li>
            <li>Reports you submit (location, photos) are <strong>publicly visible</strong> in the Leuven Go feed and shared with Leuven city operations.</li>
            <li>Submitted photos are <strong>analysed by AI</strong> (Google Gemini) to generate a cleanliness score.</li>
            <li>Anonymised data may be used for <strong>research and predictive mapping</strong> to improve city cleanliness.</li>
            <li>You can request deletion of your account and data at any time.</li>
          </ul>
          <label className="consent-check">
            <input type="checkbox" checked={consent} onChange={(e) => setConsent(e.target.checked)} />
            <span>I have read and agree to the data use described above.</span>
          </label>
        </fieldset>

        {error && (
          <div role="alert" className="form-error">
            {error}
          </div>
        )}

        <button type="submit" className="btn btn-primary btn-block" disabled={loading || !consent}>
          {loading ? "Creating account…" : "Create account"}
        </button>
      </form>
    </AuthCard>
  );
}
