import { Navigate, useLocation } from "react-router";
import type { ReactNode } from "react";
import { useAuth } from "../context/auth";
import type { Role } from "../api/types";

interface Props {
  children: ReactNode;
  /** If set, only users with this role get in; others see an explanation. */
  role?: Role;
}

export function ProtectedRoute({ children, role }: Props) {
  const { user } = useAuth();
  const location = useLocation();

  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (role && user.role !== role) {
    return (
      <div className="container" style={{ maxWidth: 520, marginTop: 60 }}>
        <div className="card" style={{ padding: 32, textAlign: "center" }}>
          <h2 style={{ marginBottom: 8 }}>Crew only</h2>
          <p className="muted">
            The operations queue is for city cleaning crews. Log in with the demo crew account
            shown on the login page to try it.
          </p>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}
