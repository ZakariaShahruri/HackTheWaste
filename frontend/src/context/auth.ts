import { createContext, useContext } from "react";
import type { AuthUser } from "../api/types";

export interface AuthContextType {
  user: AuthUser | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, facultyShortCode?: string) => Promise<void>;
  logout: () => Promise<void>;
  /** Updates the shown balance after spending points, without a round trip. */
  setPoints: (points: number) => void;
}

export const AuthContext = createContext<AuthContextType | null>(null);

export function useAuth(): AuthContextType {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside <AuthProvider>");
  return ctx;
}
