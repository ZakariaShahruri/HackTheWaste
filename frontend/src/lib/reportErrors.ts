import { errorMessage, isApiError } from "../api/http";

/** What to tell someone whose browser report didn't go through. */
export function reportErrorMessage(e: unknown): string {
  if (isApiError(e, 429)) {
    return `The browser demo takes a few reports an hour per visitor. Try again ${waitFor(e.retryAfterSeconds)}.`;
  }
  if (isApiError(e, 413)) return "That photo is too large. Try one under 5 MB.";
  return errorMessage(e, "Sending the report failed");
}

function waitFor(seconds: number | undefined): string {
  if (!seconds) return "later";
  const minutes = Math.ceil(seconds / 60);
  return minutes <= 1 ? "in a minute" : `in ${minutes} minutes`;
}
