import { render, screen } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { WhatsAppReportCTA } from "./WhatsAppReportCTA";

vi.mock("../lib/whatsapp", () => ({
  WHATSAPP_JOIN_CODE: "",
  isWhatsAppConfigured: () => true,
  buildWhatsAppLink: () => "https://wa.me/32400000000",
}));

it("names the QR code for screen readers", () => {
  render(<WhatsAppReportCTA variant="full" />);

  expect(screen.getByRole("img", { name: /qr code/i })).toBeInTheDocument();
});
