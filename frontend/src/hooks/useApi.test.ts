import { act, renderHook, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { useApi } from "./useApi";

function deferred<T>() {
  let resolve!: (v: T) => void;
  let reject!: (e: unknown) => void;
  const promise = new Promise<T>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

describe("useApi", () => {
  afterEach(() => vi.useRealTimers());

  it("starts loading, then exposes the result", async () => {
    const { result } = renderHook(() => useApi(async () => 42));
    expect(result.current.loading).toBe(true);
    await waitFor(() => expect(result.current.data).toBe(42));
    expect(result.current.loading).toBe(false);
    expect(result.current.error).toBeNull();
    expect(result.current.lastUpdated).toBeInstanceOf(Date);
  });

  it("surfaces errors with their message", async () => {
    const { result } = renderHook(() =>
      useApi(async () => {
        throw new Error("boom");
      }),
    );
    await waitFor(() => expect(result.current.error).toBe("boom"));
    expect(result.current.loading).toBe(false);
    expect(result.current.data).toBeNull();
  });

  it("ignores a slow response that resolves after a newer one", async () => {
    const first = deferred<string>();
    const second = deferred<string>();
    const pending = [first, second];
    const signals: AbortSignal[] = [];

    const { result, rerender } = renderHook(
      ({ k }) =>
        useApi(
          (signal) => {
            signals.push(signal);
            return pending.shift()!.promise;
          },
          { key: k },
        ),
      { initialProps: { k: "a" } },
    );

    rerender({ k: "b" });
    expect(signals[0].aborted).toBe(true);

    await act(async () => second.resolve("fresh"));
    await act(async () => first.resolve("stale"));

    expect(result.current.data).toBe("fresh");
    expect(result.current.loading).toBe(false);
  });

  it("reports loading again when the key changes, keeping previous data", async () => {
    const next = deferred<string>();
    let call = 0;
    const { result, rerender } = renderHook(
      ({ k }) => useApi(() => (call++ === 0 ? Promise.resolve("one") : next.promise), { key: k }),
      { initialProps: { k: 1 } },
    );
    await waitFor(() => expect(result.current.data).toBe("one"));

    rerender({ k: 2 });
    expect(result.current.loading).toBe(true);
    expect(result.current.data).toBe("one");

    await act(async () => next.resolve("two"));
    expect(result.current.data).toBe("two");
    expect(result.current.loading).toBe(false);
  });

  it("polls on an interval", async () => {
    vi.useFakeTimers();
    const loader = vi.fn(async () => "x");
    renderHook(() => useApi(loader, { pollMs: 1000 }));
    expect(loader).toHaveBeenCalledTimes(1);
    await act(async () => vi.advanceTimersByTime(3000));
    expect(loader).toHaveBeenCalledTimes(4);
  });

  it("refetch re-runs the loader", async () => {
    const loader = vi.fn(async () => "x");
    const { result } = renderHook(() => useApi(loader));
    await waitFor(() => expect(result.current.data).toBe("x"));
    act(() => result.current.refetch());
    await waitFor(() => expect(loader).toHaveBeenCalledTimes(2));
  });
});
