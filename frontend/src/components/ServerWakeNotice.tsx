import { useSyncExternalStore } from "react";
import { hasSlowReads, subscribeToSlowReads } from "../api/slowReads";

/** Tells visitors the backend is starting, so a slow first load doesn't look broken. */
export function ServerWakeNotice() {
  const waking = useSyncExternalStore(subscribeToSlowReads, hasSlowReads);

  // The live region stays mounted so screen readers announce the notice when it appears.
  return (
    <div role="status" className="wake-notice-region">
      {waking && (
        <div className="wake-notice">
          <span className="wake-spinner" aria-hidden />
          <div>
            <strong>Waking up the demo server…</strong>
            <div className="wake-notice-detail">
              It sleeps when nobody is using it, which keeps it free to host. The first load takes
              about 20 seconds.
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
