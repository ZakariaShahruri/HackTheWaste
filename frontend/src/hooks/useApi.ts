import { useCallback, useEffect, useLayoutEffect, useRef, useState } from "react";
import { errorMessage } from "../api/http";

export interface AsyncState<T> {
  /** Latest successful result. Kept while a newer request is in flight. */
  data: T | null;
  /** True until the first result (or error) for the current key arrives. */
  loading: boolean;
  error: string | null;
  /** When `data` was last refreshed successfully. */
  lastUpdated: Date | null;
  refetch: () => void;
}

export interface UseApiOptions {
  /** Re-runs the loader when this value changes (e.g. a selected filter). */
  key?: unknown;
  /** Re-runs the loader every N ms while the tab is visible and no request is in flight. */
  pollMs?: number;
}

type Loader<T> = (signal: AbortSignal) => Promise<T>;

interface Snapshot<T> {
  key: unknown;
  data: T | null;
  error: string | null;
  lastUpdated: Date | null;
}

const NOTHING_LOADED = Symbol("nothing-loaded");

/**
 * Fetches data with a loader, optionally polling. Each new request aborts the
 * previous one, and responses that arrive out of order are dropped, so the UI
 * never flips back to stale data.
 */
export function useApi<T>(loader: Loader<T>, options: UseApiOptions = {}): AsyncState<T> {
  const { key, pollMs } = options;

  const loaderRef = useRef(loader);
  useLayoutEffect(() => {
    loaderRef.current = loader;
  });

  const [snapshot, setSnapshot] = useState<Snapshot<T>>({
    key: NOTHING_LOADED,
    data: null,
    error: null,
    lastUpdated: null,
  });

  const sequenceRef = useRef(0);
  const controllerRef = useRef<AbortController | null>(null);

  const run = useCallback(async (forKey: unknown) => {
    controllerRef.current?.abort();
    const controller = new AbortController();
    controllerRef.current = controller;
    const sequence = ++sequenceRef.current;

    try {
      const data = await loaderRef.current(controller.signal);
      if (sequence !== sequenceRef.current) return;
      setSnapshot({ key: forKey, data, error: null, lastUpdated: new Date() });
    } catch (e) {
      if (controller.signal.aborted || sequence !== sequenceRef.current) return;
      setSnapshot((prev) => ({ ...prev, key: forKey, error: errorMessage(e) }));
    } finally {
      if (controllerRef.current === controller) controllerRef.current = null;
    }
  }, []);

  useEffect(() => {
    void run(key);
    const timer =
      pollMs && pollMs > 0
        ? setInterval(() => {
            // A slow request, e.g. while the server wakes up, is left to finish instead of restarted.
            if (document.visibilityState === "visible" && !controllerRef.current) void run(key);
          }, pollMs)
        : undefined;
    return () => {
      clearInterval(timer);
      controllerRef.current?.abort();
    };
  }, [run, key, pollMs]);

  const refetch = useCallback(() => void run(key), [run, key]);

  return {
    data: snapshot.data,
    loading: !Object.is(snapshot.key, key),
    error: Object.is(snapshot.key, key) ? snapshot.error : null,
    lastUpdated: snapshot.lastUpdated,
    refetch,
  };
}
