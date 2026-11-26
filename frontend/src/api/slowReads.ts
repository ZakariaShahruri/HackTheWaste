/**
 * Counts reads that have taken longer than a warm server ever does. The backend
 * sleeps when nobody is using it and takes about 20 seconds to wake, so a read
 * this slow almost always means it is starting up.
 */
export const SLOW_READ_MS = 4000;

let slowReads = 0;
const listeners = new Set<() => void>();

function notify() {
  listeners.forEach((listener) => listener());
}

/** Starts timing a read. Call the returned function once it has settled. */
export function trackRead(): () => void {
  let slow = false;
  const timer = setTimeout(() => {
    slow = true;
    slowReads++;
    notify();
  }, SLOW_READ_MS);

  return () => {
    clearTimeout(timer);
    if (slow) {
      slowReads--;
      notify();
    }
  };
}

export function subscribeToSlowReads(listener: () => void): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

export function hasSlowReads(): boolean {
  return slowReads > 0;
}
