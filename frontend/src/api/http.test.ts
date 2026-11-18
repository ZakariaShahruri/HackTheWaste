import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError, errorMessage, isApiError, request, setUnauthorizedListener } from "./http";
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
    writeStoredUser({ id: 1, email: "a@b.c", role: "USER", token: "t0k" });
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

  it("falls back to a generic message for non-JSON errors", async () => {
    fetchMock.mockResolvedValue(new Response("oops", { status: 502 }));
    const err = await request("/x").catch((e: unknown) => e);
    expect(isApiError(err, 502)).toBe(true);
    expect(errorMessage(err)).toMatch(/502/);
  });

  it("notifies the unauthorized listener only for authenticated calls", async () => {
    const listener = vi.fn();
    setUnauthorizedListener(listener);

    fetchMock.mockResolvedValue(jsonResponse(401, {}));
    await request("/x").catch(() => {});
    expect(listener).not.toHaveBeenCalled();

    writeStoredUser({ id: 1, email: "a@b.c", role: "USER", token: "expired" });
    fetchMock.mockResolvedValue(jsonResponse(401, {}));
    await request("/x").catch(() => {});
    expect(listener).toHaveBeenCalledOnce();
  });
});
