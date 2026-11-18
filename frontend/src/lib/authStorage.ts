import type { AuthUser } from "../api/types";

const STORAGE_KEY = "leuvengo.auth";

export function readStoredUser(): AuthUser | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const user = JSON.parse(raw) as AuthUser;
    // Sessions stored before points existed; /auth/me fills in the real balance.
    return { ...user, points: user.points ?? 0 };
  } catch {
    return null;
  }
}

export function writeStoredUser(user: AuthUser | null): void {
  try {
    if (user) localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
    else localStorage.removeItem(STORAGE_KEY);
  } catch {
    // Storage can be unavailable (private mode, blocked cookies) — the session
    // then simply lasts for this page load.
  }
}
