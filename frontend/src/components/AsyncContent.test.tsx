import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import type { AsyncState } from "../hooks/useApi";
import { AsyncContent } from "./AsyncContent";
import { EmptyState } from "./States";

function state<T>(overrides: Partial<AsyncState<T>>): AsyncState<T> {
  return { data: null, loading: false, error: null, lastUpdated: null, refetch: vi.fn(), ...overrides };
}

function renderList(s: AsyncState<string[]>) {
  return render(
    <AsyncContent state={s} empty={<EmptyState title="Nothing here" />} loading={<p>Loading…</p>}>
      {(items) => <ul>{items.map((i) => <li key={i}>{i}</li>)}</ul>}
    </AsyncContent>,
  );
}

describe("AsyncContent", () => {
  it("shows the placeholder until the first result", () => {
    renderList(state({ loading: true }));

    expect(screen.getByText("Loading…")).toBeInTheDocument();
  });

  it("shows the error with a retry when nothing has loaded", async () => {
    const refetch = vi.fn();
    renderList(state({ error: "Backend unreachable", refetch }));

    expect(screen.getByRole("alert")).toHaveTextContent("Backend unreachable");
    await userEvent.click(screen.getByRole("button", { name: "Try again" }));
    expect(refetch).toHaveBeenCalledOnce();
  });

  it("keeps showing loaded data when a later refresh fails", () => {
    renderList(state({ data: ["Oude Markt"], error: "Backend unreachable" }));

    expect(screen.getByText("Oude Markt")).toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("shows the empty state for an empty list", () => {
    renderList(state({ data: [] }));

    expect(screen.getByText("Nothing here")).toBeInTheDocument();
  });
});
