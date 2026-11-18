import { describe, expect, it } from "vitest";
import { ApiError } from "../api/http";
import { reportErrorMessage } from "./reportErrors";

describe("reportErrorMessage", () => {
  it("says how long to wait after too many reports", () => {
    expect(reportErrorMessage(new ApiError(429, "Too many requests", 1150))).toBe(
      "The browser demo takes a few reports an hour per visitor. Try again in 20 minutes.",
    );
    expect(reportErrorMessage(new ApiError(429, "Too many requests", 30))).toMatch(/in a minute\.$/);
    expect(reportErrorMessage(new ApiError(429, "Too many requests"))).toMatch(/later\.$/);
  });

  it("explains an upload that's too large", () => {
    expect(reportErrorMessage(new ApiError(413, "Maximum upload size exceeded"))).toBe(
      "That photo is too large. Try one under 5 MB.",
    );
  });

  it("passes on the server's explanation otherwise", () => {
    expect(reportErrorMessage(new ApiError(422, "Our AI doesn't think this is a genuine on-site photo"))).toBe(
      "Our AI doesn't think this is a genuine on-site photo",
    );
  });
});
