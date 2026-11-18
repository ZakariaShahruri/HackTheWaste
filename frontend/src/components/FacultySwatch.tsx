/** A clan's colour next to its name, or a dash for streets no clan holds. */
export function FacultySwatch({ name, color }: { name?: string; color?: string }) {
  if (!name) return <span className="muted">—</span>;
  return (
    <span className="swatch-label">
      <span className="swatch" style={{ background: color ?? "var(--fg-soft)" }} aria-hidden />
      {name}
    </span>
  );
}
