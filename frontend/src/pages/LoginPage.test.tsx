import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "../api/http";
import LoginPage from "./LoginPage";

const { auth } = vi.hoisted(() => ({ auth: { login: vi.fn(), loading: false } }));
vi.mock("../context/auth", () => ({ useAuth: () => auth }));

function renderPage() {
  render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>,
  );
}

describe("LoginPage", () => {
  beforeEach(() => vi.clearAllMocks());

  it("logs in with the demo crew account", async () => {
    auth.login.mockResolvedValue(undefined);
    renderPage();

    await userEvent.click(screen.getByRole("button", { name: "Fill in" }));
    await userEvent.click(screen.getByRole("button", { name: "Log in" }));

    expect(auth.login).toHaveBeenCalledWith("crew@leuvengo.demo", "cleanleuven");
  });

  it("announces a wrong password", async () => {
    auth.login.mockRejectedValue(new ApiError(401, "Unauthorized"));
    renderPage();

    await userEvent.type(screen.getByLabelText("Email"), "a@kuleuven.be");
    await userEvent.type(screen.getByLabelText("Password"), "wrong-password");
    await userEvent.click(screen.getByRole("button", { name: "Log in" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Invalid email or password");
    expect(screen.getByRole("heading", { level: 1, name: "Welcome back" })).toBeInTheDocument();
  });
});
