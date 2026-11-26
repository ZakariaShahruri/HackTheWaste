import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { SLOW_READ_MS, hasSlowReads, subscribeToSlowReads, trackRead } from "./slowReads";

describe("slow reads", () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it("flags a read only once it is slow, and clears it when it settles", () => {
    const listener = vi.fn();
    const unsubscribe = subscribeToSlowReads(listener);
    const settled = trackRead();

    vi.advanceTimersByTime(SLOW_READ_MS - 1);
    expect(hasSlowReads()).toBe(false);

    vi.advanceTimersByTime(1);
    expect(hasSlowReads()).toBe(true);

    settled();
    expect(hasSlowReads()).toBe(false);
    expect(listener).toHaveBeenCalledTimes(2);
    unsubscribe();
  });

  it("never flags a read that settles in time", () => {
    const listener = vi.fn();
    const unsubscribe = subscribeToSlowReads(listener);

    trackRead()();
    vi.advanceTimersByTime(SLOW_READ_MS * 2);

    expect(hasSlowReads()).toBe(false);
    expect(listener).not.toHaveBeenCalled();
    unsubscribe();
  });

  it("stays flagged while any slow read is still waiting", () => {
    const first = trackRead();
    const second = trackRead();
    vi.advanceTimersByTime(SLOW_READ_MS);

    first();
    expect(hasSlowReads()).toBe(true);
    second();
    expect(hasSlowReads()).toBe(false);
  });
});
