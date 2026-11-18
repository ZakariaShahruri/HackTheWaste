const DARK_TEXT = "#0f172a";
const LIGHT_TEXT = "#ffffff";

function channel(value: number): number {
  const c = value / 255;
  return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
}

/** WCAG relative luminance of a #rrggbb colour. */
export function luminance(hex: string): number {
  const n = parseInt(hex.slice(1), 16);
  return 0.2126 * channel((n >> 16) & 255) + 0.7152 * channel((n >> 8) & 255) + 0.0722 * channel(n & 255);
}

export function contrast(a: string, b: string): number {
  const [hi, lo] = [luminance(a), luminance(b)].sort((x, y) => y - x);
  return (hi + 0.05) / (lo + 0.05);
}

/**
 * White or near-black, whichever reads better on the given background. Clan
 * colours come from the database, so their text colour can't be fixed in CSS.
 */
export function readableText(background: string): string {
  if (!/^#[0-9a-f]{6}$/i.test(background)) return LIGHT_TEXT;
  return contrast(background, LIGHT_TEXT) >= contrast(background, DARK_TEXT) ? LIGHT_TEXT : DARK_TEXT;
}

/** Inline style for a label filled with a colour. */
export function filled(background: string) {
  return { background, color: readableText(background) };
}
