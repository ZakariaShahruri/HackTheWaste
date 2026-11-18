import { useState } from "react";
import { Link, useNavigate } from "react-router";
import { useAuth } from "../context/auth";
import { errorMessage, isApiError } from "../api/http";
import { useApi } from "../hooks/useApi";
import { api } from "../api/client";
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
    <div className="container" style={{ maxWidth: 480, marginTop: 60 }}>
      <div className="card" style={{ padding: 32 }}>
        <h2 style={{ marginBottom: 6 }}>Create account</h2>
        <p className="muted" style={{ marginBottom: 24, fontSize: 14 }}>
          Join Leuven Go and start earning points for your clan
        </p>

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
              <p style={{ marginTop: 8, fontSize: 13, color: "var(--fg-soft)" }}>
                Selected: <strong>{faculties?.find((f) => f.shortCode === clan)?.name}</strong>
              </p>
            )}
          </div>

          {/* Data consent */}
          <div
            style={{
              border: "1px solid var(--border)",
              borderRadius: 10,
              padding: "14px 16px",
              background: "var(--bg-elev, #f9fafb)",
            }}
          >
            <p style={{ fontWeight: 600, fontSize: 14, marginBottom: 8 }}>
              Data consent
            </p>
            <ul
              style={{
                fontSize: 13,
                color: "var(--fg-soft)",
                paddingLeft: 18,
                marginBottom: 12,
                lineHeight: 1.6,
              }}
            >
              <li>Your <strong>email and clan</strong> are stored to personalise your experience and credit your reports to the right team.</li>
              <li>Reports you submit (location, photos) are <strong>publicly visible</strong> in the Leuven Go feed and shared with Leuven city operations.</li>
              <li>Submitted photos are <strong>analysed by AI</strong> (Google Gemini) to generate a cleanliness score.</li>
              <li>Anonymised data may be used for <strong>research and predictive mapping</strong> to improve city cleanliness.</li>
              <li>You can request deletion of your account and data at any time.</li>
            </ul>
            <label style={{ display: "flex", alignItems: "flex-start", gap: 10, cursor: "pointer" }}>
              <input
                type="checkbox"
                checked={consent}
                onChange={(e) => setConsent(e.target.checked)}
                style={{ marginTop: 2, flexShrink: 0, width: 16, height: 16, cursor: "pointer" }}
              />
              <span style={{ fontSize: 13 }}>
                I have read and agree to the data use described above.
              </span>
            </label>
          </div>

          {error && (
            <div
              style={{
                padding: "10px 14px",
                background: "#fee2e2",
                color: "#991b1b",
                borderRadius: 10,
                fontSize: 14,
              }}
            >
              {error}
            </div>
          )}

          <button
            type="submit"
            className="btn btn-primary"
            disabled={loading || !consent}
            style={{ width: "100%", justifyContent: "center", marginTop: 4 }}
          >
            {loading ? "Creating account…" : "Create account"}
          </button>
        </form>

        <p style={{ marginTop: 20, textAlign: "center", fontSize: 14, color: "var(--fg-soft)" }}>
          Already have an account?{" "}
          <Link to="/login" style={{ color: "var(--primary)", fontWeight: 600 }}>
            Log in
          </Link>
        </p>
      </div>
    </div>
  );
}
