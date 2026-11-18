import type { FacultyDto } from "../api/types";

interface Props {
  faculties: FacultyDto[];
  selected?: string;
  /** Called with the clan's code, or undefined when the selected clan is clicked again. */
  onSelect: (shortCode: string | undefined) => void;
  /** Adds a first pill that clears the selection, e.g. "All clans". */
  noneLabel?: string;
  label: string;
}

/** A row of clan buttons in their own colours; one can be selected at a time. */
export function ClanPills({ faculties, selected, onSelect, noneLabel, label }: Props) {
  return (
    <div className="clan-pills" role="group" aria-label={label}>
      {noneLabel && (
        <button
          type="button"
          className={`btn btn-sm clan-pill clan-pill-none${selected ? "" : " active"}`}
          aria-pressed={!selected}
          onClick={() => onSelect(undefined)}
        >
          {noneLabel}
        </button>
      )}
      {faculties.map((f) => {
        const active = selected === f.shortCode;
        return (
          <button
            key={f.id}
            type="button"
            className={`btn btn-sm clan-pill${active ? " active" : ""}`}
            aria-pressed={active}
            title={f.name}
            style={{ ["--clan" as string]: f.color }}
            onClick={() => onSelect(active ? undefined : f.shortCode)}
          >
            {f.emoji} {f.shortCode}
          </button>
        );
      })}
    </div>
  );
}
