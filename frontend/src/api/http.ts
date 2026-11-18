import { readStoredUser } from "../lib/authStorage";

const API_ORIGIN = import.meta.env.VITE_API_BASE_URL ?? "";
const BASE = API_ORIGIN + "/api";

/**
 * Photos stored by the backend come back as "/api/..." paths. In production the
 * frontend runs on a different host, so those need the API origin in front.
 */
export function assetUrl(url: string): string {
  return url.startsWith("/api/") ? API_ORIGIN + url : url;
}

/** Error thrown for any non-2xx response, carrying the HTTP status. */
export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

export const SERVER_UNREACHABLE = "The Leuven Go server can't be reached right now. Try again in a moment.";

/** What proxies and hosts answer while the backend is down or starting up. */
const UNREACHABLE_STATUSES = [502, 503, 504];

export function isApiError(e: unknown, status?: number): e is ApiError {
  return e instanceof ApiError && (status === undefined || e.status === status);
}

export function errorMessage(e: unknown, fallback = "Something went wrong"): string {
  return e instanceof Error && e.message ? e.message : fallback;
}

type UnauthorizedListener = () => void;
let onUnauthorized: UnauthorizedListener | null = null;

/** Registers a callback fired whenever an authenticated request returns 401. */
export function setUnauthorizedListener(listener: UnauthorizedListener | null): void {
  onUnauthorized = listener;
}

export interface RequestOptions {
  method?: string;
  json?: unknown;
  body?: BodyInit;
  signal?: AbortSignal;
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers();
  let body = options.body;
  if (options.json !== undefined) {
    headers.set("Content-Type", "application/json");
    body = JSON.stringify(options.json);
  }
  const token = readStoredUser()?.token;
  if (token) headers.set("Authorization", `Bearer ${token}`);

  let res: Response;
  try {
    res = await fetch(`${BASE}${path}`, {
      method: options.method ?? "GET",
      headers,
      body,
      signal: options.signal,
    });
  } catch (e) {
    if (options.signal?.aborted) throw e;
    throw new ApiError(0, SERVER_UNREACHABLE);
  }

  if (!res.ok) {
    if (res.status === 401 && token) onUnauthorized?.();
    throw new ApiError(res.status, await readErrorMessage(res));
  }
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

async function readErrorMessage(res: Response): Promise<string> {
  if (UNREACHABLE_STATUSES.includes(res.status)) return SERVER_UNREACHABLE;
  try {
    const body = (await res.json()) as { message?: string };
    if (body.message) return body.message;
  } catch {
    // not JSON — fall through to the status text
  }
  return res.statusText || `Request failed (${res.status})`;
}
