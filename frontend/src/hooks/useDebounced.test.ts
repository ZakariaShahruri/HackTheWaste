import { act, renderHook } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { useDebounced } from "./useDebounced";

describe("useDebounced", () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it("only passes on the value once it stops changing", () => {
    const { result, rerender } = renderHook(({ value }) => useDebounced(value, 250), {
      initialProps: { value: 10 },
    });

    rerender({ value: 11 });
    act(() => vi.advanceTimersByTime(200));
    rerender({ value: 12 });
    act(() => vi.advanceTimersByTime(200));
    expect(result.current).toBe(10);

    act(() => vi.advanceTimersByTime(50));
    expect(result.current).toBe(12);
  });
});
