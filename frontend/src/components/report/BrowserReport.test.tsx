import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "../../api/http";
import type { AuthUser, DemoReportResult } from "../../api/types";
import { BrowserReport } from "./BrowserReport";

const { api, auth } = vi.hoisted(() => ({
  api: {
    faculties: vi.fn(),
    submitDemoReport: vi.fn(),
    me: vi.fn(),
  },
  auth: { user: null as AuthUser | null, setPoints: vi.fn() },
}));

vi.mock("../../api/client", () => ({ api }));
vi.mock("../../context/auth", () => ({ useAuth: () => auth }));
vi.mock("../../lib/photos", () => ({ shrinkPhoto: (file: Blob) => Promise.resolve(file) }));
// The real picker is a Leaflet map; a tap on Oude Markt is all this flow needs.
vi.mock("./LocationPicker", () => ({
  LocationPicker: ({ onChange }: { onChange: (spot: [number, number]) => void }) => (
    <button type="button" onClick={() => onChange([50.8785, 4.7])}>
      Pick Oude Markt
    </button>
  ),
}));

const verdict: DemoReportResult = {
  reportId: 9,
  hotspotId: 3,
  photoUrl: "/api/photos/abc",
  aiAvailable: true,
  score: 28,
  label: "Dirty",
  description: "Cans and cups around the benches.",
  tags: ["cans", "cups"],
};

const photo = new File(["jpeg"], "litter.jpg", { type: "image/jpeg" });

function renderForm() {
  return render(
    <MemoryRouter>
      <BrowserReport />
    </MemoryRouter>,
  );
}

async function fillIn() {
  await userEvent.click(screen.getByRole("button", { name: "Pick Oude Markt" }));
  await userEvent.upload(screen.getByLabelText(/take a photo/i), photo);
}

describe("BrowserReport", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    auth.user = null;
    api.faculties.mockResolvedValue([
      { id: 1, name: "Ekonomika", shortCode: "EKO", color: "#10b981", emoji: "📊", members: 0, points: 0 },
    ]);
    URL.createObjectURL = vi.fn(() => "blob:preview");
    URL.revokeObjectURL = vi.fn();
  });

  it("only sends once there's a spot and a photo", async () => {
    renderForm();
    const send = screen.getByRole("button", { name: "Send report" });
    expect(send).toBeDisabled();

    await fillIn();

    expect(send).toBeEnabled();
    expect(screen.getByRole("img", { name: "The photo you picked" })).toHaveAttribute("src", "blob:preview");
  });

  it("sends the report for the chosen clan and shows the verdict", async () => {
    api.submitDemoReport.mockResolvedValue(verdict);
    renderForm();
    await fillIn();
    await userEvent.selectOptions(screen.getByRole("combobox", { name: "Clan" }), await screen.findByRole("option", { name: /Ekonomika/ }));

    await userEvent.click(screen.getByRole("button", { name: "Send report" }));

    expect(api.submitDemoReport).toHaveBeenCalledWith({ photo, lat: 50.8785, lng: 4.7, clan: "EKO" });
    expect(await screen.findByText("Dirty")).toBeInTheDocument();
    expect(screen.getByText("Cans and cups around the benches.")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "See it on the map" })).toHaveAttribute("href", "/student");
  });

  it("says when the AI was offline instead of showing its placeholder score", async () => {
    api.submitDemoReport.mockResolvedValue({ ...verdict, aiAvailable: false, score: 50, label: "AI offline, demo score" });
    renderForm();
    await fillIn();

    await userEvent.click(screen.getByRole("button", { name: "Send report" }));

    expect(await screen.findByText(/couldn't be reached/)).toBeInTheDocument();
    expect(screen.queryByText("50")).not.toBeInTheDocument();
  });

  it("tells a logged-in reporter what they earned", async () => {
    auth.user = { id: 4, email: "a@kuleuven.be", role: "USER", points: 10, token: "t", facultyShortCode: "EKO" };
    api.submitDemoReport.mockResolvedValue(verdict);
    api.me.mockResolvedValue({ ...auth.user, points: 46 });
    renderForm();
    await fillIn();

    await userEvent.click(screen.getByRole("button", { name: "Send report" }));

    expect(await screen.findByText("+36 pts for you")).toBeInTheDocument();
    expect(auth.setPoints).toHaveBeenCalledWith(46);
  });

  it("says how long to wait when the hourly limit is reached", async () => {
    api.submitDemoReport.mockRejectedValue(new ApiError(429, "Too many requests", 600));
    renderForm();
    await fillIn();

    await userEvent.click(screen.getByRole("button", { name: "Send report" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Try again in 10 minutes.");
    expect(screen.getByRole("button", { name: "Send report" })).toBeEnabled();
  });
});
