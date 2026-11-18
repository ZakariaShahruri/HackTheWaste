import { request } from "./http";
import type {
  AuthUser,
  FacultyDto,
  FeedPage,
  HotspotDto,
  LeaderboardEntry,
  PredictPoint,
  RewardItemDto,
  StatsDto,
  StreetSegmentDto,
  WorkOrderDto,
} from "./types";

type Signal = AbortSignal | undefined;

export const api = {
  register: (email: string, password: string, facultyShortCode?: string) =>
    request<AuthUser>("/auth/register", {
      method: "POST",
      json: { email, password, facultyShortCode, consentGiven: true },
    }),
  login: (email: string, password: string) =>
    request<AuthUser>("/auth/login", { method: "POST", json: { email, password } }),
  me: (signal?: Signal) => request<AuthUser>("/auth/me", { signal }),
  logout: () => request<void>("/auth/logout", { method: "POST" }),

  stats: (signal?: Signal) => request<StatsDto>("/stats", { signal }),
  hotspots: (signal?: Signal) => request<HotspotDto[]>("/hotspots", { signal }),
  activeHotspots: (signal?: Signal) => request<HotspotDto[]>("/hotspots/active", { signal }),
  segments: (signal?: Signal) => request<StreetSegmentDto[]>("/segments", { signal }),
  faculties: (signal?: Signal) => request<FacultyDto[]>("/faculties", { signal }),
  leaderboard: (signal?: Signal) => request<LeaderboardEntry[]>("/leaderboard", { signal }),
  workOrders: (signal?: Signal) => request<WorkOrderDto[]>("/work-orders", { signal }),
  completeWorkOrder: (id: number) =>
    request<WorkOrderDto>(`/work-orders/${id}/complete`, { method: "POST" }),
  rewards: (signal?: Signal) => request<RewardItemDto[]>("/rewards", { signal }),
  feed: (page = 0, size = 12, clan?: string, signal?: Signal) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (clan) params.set("clan", clan);
    return request<FeedPage>(`/feed?${params}`, { signal });
  },

  predictGrid: (at: string, steps = 30, signal?: Signal) => {
    const params = new URLSearchParams({ at, steps: String(steps) });
    return request<PredictPoint[]>(`/predictions/grid?${params}`, { signal });
  },
};
