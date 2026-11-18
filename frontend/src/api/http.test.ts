import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError, SERVER_UNREACHABLE, errorMessage, isApiError, request, setUnauthorizedListener } from "./http";
import { writeStoredUser } from "../lib/authStorage";

function jsonResponse(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

describe("request", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    vi.stubGlobal("fetch", fetchMock);
    fetchMock.mockReset();
    writeStoredUser(null);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    setUnauthorizedListener(null);
  });

  it("parses JSON on success", async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, { ok: true }));
    await expect(request("/x")).resolves.toEqual({ ok: true });
    expect(fetchMock.mock.calls[0][0]).toBe("/api/x");
  });

  it("sends JSON bodies and the bearer token", async () => {
    writeStoredUser({ id: 1, email: "a@b.c", role: "USER", points: 0, token: "t0k" });
    fetchMock.mockResolvedValue(jsonResponse(200, {}));
    await request("/x", { method: "POST", json: { a: 1 } });

    const init = fetchMock.mock.calls[0][1] as RequestInit;
    const headers = init.headers as Headers;
    expect(init.method).toBe("POST");
    expect(init.body).toBe('{"a":1}');
    expect(headers.get("Content-Type")).toBe("application/json");
    expect(headers.get("Authorization")).toBe("Bearer t0k");
  });

  it("throws ApiError with the server message and status", async () => {
    fetchMock.mockResolvedValue(jsonResponse(409, { message: "Email already registered" }));
    const err = await request("/x").catch((e: unknown) => e);
    expect(err).toBeInstanceOf(ApiError);
    expect(isApiError(err, 409)).toBe(true);
    expect(errorMessage(err)).toBe("Email already registered");
  });

  it("falls back to the status for non-JSON errors", async () => {
    fetchMock.mockResolvedValue(new Response("oops", { status: 500 }));
    const err = await request("/x").catch((e: unknown) => e);
    expect(isApiError(err, 500)).toBe(true);
    expect(errorMessage(err)).toMatch(/500/);
  });

  it.each([502, 503, 504])("explains a %i from the proxy as the server being unreachable", async (status) => {
    fetchMock.mockResolvedValue(new Response("Bad Gateway", { status }));
    const err = await request("/x").catch((e: unknown) => e);
    expect(isApiError(err, status)).toBe(true);
    expect(errorMessage(err)).toBe(SERVER_UNREACHABLE);
  });

  it("explains a failed connection as the server being unreachable", async () => {
    fetchMock.mockRejectedValue(new TypeError("Failed to fetch"));
    const err = await request("/x").catch((e: unknown) => e);
    expect(isApiError(err, 0)).toBe(true);
    expect(errorMessage(err)).toBe(SERVER_UNREACHABLE);
  });

  it("reads how long to wait from a 429", async () => {
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({ message: "Too many requests" }), { status: 429, headers: { "Retry-After": "1200" } }),
    );
    const err = (await request("/x").catch((e: unknown) => e)) as ApiError;
    expect(err.status).toBe(429);
    expect(err.retryAfterSeconds).toBe(1200);
  });

  it("lets an abort through untouched", async () => {
    const controller = new AbortController();
    controller.abort();
    fetchMock.mockRejectedValue(new DOMException("Aborted", "AbortError"));
    const err = await request("/x", { signal: controller.signal }).catch((e: unknown) => e);
    expect(err).not.toBeInstanceOf(ApiError);
  });

  it("notifies the unauthorized listener only for authenticated calls", async () => {
    const listener = vi.fn();
    setUnauthorizedListener(listener);

    fetchMock.mockResolvedValue(jsonResponse(401, {}));
    await request("/x").catch(() => {});
    expect(listener).not.toHaveBeenCalled();

    writeStoredUser({ id: 1, email: "a@b.c", role: "USER", points: 0, token: "expired" });
    fetchMock.mockResolvedValue(jsonResponse(401, {}));
    await request("/x").catch(() => {});
    expect(listener).toHaveBeenCalledOnce();
  });
});

describe("assetUrl", () => {
  afterEach(() => {
    vi.unstubAllEnvs();
    vi.resetModules();
  });

  it("leaves absolute urls alone", async () => {
    const { assetUrl } = await import("./http");
    expect(assetUrl("https://blob.example/p.jpg")).toBe("https://blob.example/p.jpg");
  });

  it("prefixes backend paths with the api origin", async () => {
    vi.stubEnv("VITE_API_BASE_URL", "https://api.leuvengo.example");
    vi.resetModules();
    const { assetUrl } = await import("./http");
    expect(assetUrl("/api/photos/abc")).toBe("https://api.leuvengo.example/api/photos/abc");
  });
});
