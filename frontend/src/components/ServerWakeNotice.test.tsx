import { act, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { SLOW_READ_MS, trackRead } from "../api/slowReads";
import { ServerWakeNotice } from "./ServerWakeNotice";

beforeEach(() => vi.useFakeTimers());
afterEach(() => vi.useRealTimers());

it("explains the wait while a read is slow, and goes away once it arrives", () => {
  render(<ServerWakeNotice />);
  const settled = trackRead();
  expect(screen.queryByText(/waking up/i)).toBeNull();

  act(() => vi.advanceTimersByTime(SLOW_READ_MS));
  expect(screen.getByRole("status")).toHaveTextContent(/waking up the demo server/i);

  act(() => settled());
  expect(screen.queryByText(/waking up/i)).toBeNull();
});
