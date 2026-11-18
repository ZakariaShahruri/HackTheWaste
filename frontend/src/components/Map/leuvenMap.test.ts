import { describe, expect, it } from "vitest";
import { isInLeuven } from "./leuvenMap";

describe("isInLeuven", () => {
  it.each([
    [50.8785, 4.7, true], // Oude Markt
    [50.864, 4.679, true], // Arenberg
    [50.8467, 4.3525, false], // Brussels Grote Markt
    [50.93, 4.79, true], // the corner itself
  ])("%d, %d → %s", (lat, lng, expected) => {
    expect(isInLeuven(lat, lng)).toBe(expected);
  });
});
