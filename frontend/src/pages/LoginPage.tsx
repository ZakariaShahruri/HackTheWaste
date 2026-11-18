import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router";
import { useAuth } from "../context/auth";
import { errorMessage, isApiError } from "../api/http";

/** Public demo account seeded by the backend (leuvengo.demo.*). */
const DEMO_CREW = { email: "crew@leuvengo.demo", password: "cleanleuven" };

export default function LoginPage() {
  const { login, loading } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: { pathname: string } } | null)?.from?.pathname ?? "/student";
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);

  function fillDemoCrew() {
    setEmail(DEMO_CREW.email);
    setPassword(DEMO_CREW.password);
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await login(email, password);
      navigate(from, { replace: true });
    } catch (err) {
      setError(isApiError(err, 401) ? "Invalid email or password" : errorMessage(err, "Login failed"));
    }
  }

  return (
    <div className="container" style={{ maxWidth: 420, marginTop: 60 }}>
      <div className="card" style={{ padding: 32 }}>
        <h2 style={{ marginBottom: 6 }}>Welcome back</h2>
        <p className="muted" style={{ marginBottom: 24, fontSize: 14 }}>
          Log in to your Leuven Go account
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
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              placeholder="••••••••"
            />
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
            disabled={loading}
            style={{ width: "100%", justifyContent: "center", marginTop: 4 }}
          >
            {loading ? "Logging in…" : "Log in"}
          </button>
        </form>

        <div className="demo-login-hint">
          <strong>Trying the demo?</strong> Log in as a city crew with{" "}
          <code>{DEMO_CREW.email}</code> / <code>{DEMO_CREW.password}</code>{" "}
          <button type="button" className="link-btn" onClick={fillDemoCrew}>
            Fill in
          </button>
        </div>

        <p style={{ marginTop: 20, textAlign: "center", fontSize: 14, color: "var(--fg-soft)" }}>
          No account yet?{" "}
          <Link to="/signup" style={{ color: "var(--primary)", fontWeight: 600 }}>
            Sign up
          </Link>
        </p>
      </div>
    </div>
  );
}
