import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import type { AuthUser } from "../api/types";
import { api } from "../api/client";
import { isApiError, setUnauthorizedListener } from "../api/http";
import { readStoredUser, writeStoredUser } from "../lib/authStorage";
import { AuthContext } from "./auth";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(readStoredUser);
  const [loading, setLoading] = useState(false);

  const persist = useCallback((u: AuthUser | null) => {
    writeStoredUser(u);
    setUser(u);
  }, []);

  // Any 401 on an authenticated call means the session is gone server-side.
  useEffect(() => {
    setUnauthorizedListener(() => persist(null));
    return () => setUnauthorizedListener(null);
  }, [persist]);

  // Re-validate a stored session once on load so a stale token doesn't
  // masquerade as being logged in.
  useEffect(() => {
    if (!readStoredUser()) return;
    const controller = new AbortController();
    api
      .me(controller.signal)
      .then((fresh) => persist({ ...fresh, token: readStoredUser()?.token ?? fresh.token }))
      .catch((e: unknown) => {
        if (isApiError(e, 401)) persist(null);
      });
    return () => controller.abort();
  }, [persist]);

  const login = useCallback(
    async (email: string, password: string) => {
      setLoading(true);
      try {
        persist(await api.login(email, password));
      } finally {
        setLoading(false);
      }
    },
    [persist],
  );

  const register = useCallback(
    async (email: string, password: string, facultyShortCode?: string) => {
      setLoading(true);
      try {
        persist(await api.register(email, password, facultyShortCode));
      } finally {
        setLoading(false);
      }
    },
    [persist],
  );

  const logout = useCallback(async () => {
    try {
      await api.logout();
    } catch {
      // token may already be invalid — clearing locally is what matters
    }
    persist(null);
  }, [persist]);

  const value = useMemo(
    () => ({ user, loading, login, register, logout }),
    [user, loading, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
