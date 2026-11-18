import { useState, type MouseEvent } from "react";
import { NavLink, useLocation, useNavigate } from "react-router";
import { useAuth } from "../context/auth";

type Mode = "landing" | "pro" | "student";

function modeFor(pathname: string): Mode {
  if (pathname.startsWith("/pro")) return "pro";
  if (["/", "/login", "/signup"].includes(pathname)) return "landing";
  return "student";
}

export function TopBar() {
  const { pathname } = useLocation();
  const mode = modeFor(pathname);
  const [open, setOpen] = useState(false);

  // On small screens the links live in a drop-down; following one closes it.
  const closeOnLink = (e: MouseEvent) => {
    if ((e.target as HTMLElement).closest("a, button")) setOpen(false);
  };

  return (
    <header className={`topbar${open ? " open" : ""}`}>
      <NavLink to="/" className="brand">
        <span className="brand-mark">LG</span>
        <span>Leuven Go</span>
      </NavLink>

      <button
        type="button"
        className="menu-toggle"
        aria-expanded={open}
        aria-controls="topbar-menu"
        aria-label={open ? "Close menu" : "Open menu"}
        onClick={() => setOpen((o) => !o)}
      >
        {open ? "✕" : "☰"}
      </button>

      <div id="topbar-menu" className="topbar-menu" onClick={closeOnLink}>
        <nav className="nav" aria-label="Main">
          {mode === "landing" && (
            <>
              <NavLink to="/pro">For city teams</NavLink>
              <NavLink to="/student">For students</NavLink>
              <NavLink to="/feed">Feed</NavLink>
              <NavLink to="/report">Report</NavLink>
            </>
          )}
          {mode === "pro" && (
            <>
              <NavLink to="/pro" end>Overview</NavLink>
              <NavLink to="/pro/operations">Operations</NavLink>
              <NavLink to="/student" className="btn btn-ghost btn-sm">Student view</NavLink>
            </>
          )}
          {mode === "student" && (
            <>
              <NavLink to="/student" end>Map</NavLink>
              <NavLink to="/student/leaderboard">Clans</NavLink>
              <NavLink to="/market">Rewards</NavLink>
              <NavLink to="/feed">Feed</NavLink>
              <NavLink to="/report">Report</NavLink>
              <NavLink to="/pro" className="btn btn-ghost btn-sm">City view</NavLink>
            </>
          )}
        </nav>
        <AuthArea />
      </div>
    </header>
  );
}

function AuthArea() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  if (!user) {
    return (
      <div className="topbar-auth">
        <NavLink to="/login" className="btn btn-ghost btn-sm">Log in</NavLink>
        <NavLink to="/signup" className="btn btn-primary btn-sm">Sign up</NavLink>
      </div>
    );
  }

  return (
    <div className="topbar-auth">
      <span className="topbar-user">
        {user.facultyShortCode ? `${user.facultyShortCode} · ` : ""}
        {user.email.split("@")[0]}
      </span>
      <span className="topbar-points">{user.points} pts</span>
      <button className="btn btn-ghost btn-sm" onClick={() => void logout().then(() => navigate("/"))}>
        Log out
      </button>
    </div>
  );
}
