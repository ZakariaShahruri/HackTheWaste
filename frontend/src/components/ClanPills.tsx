import type { FacultyDto } from "../api/types";
import { readableText } from "../lib/colors";
import { Skeleton } from "./States";

// Roughly how many clans there are, so the pills don't push the page down when they arrive.
const PLACEHOLDERS = 21;

interface Props {
  /** Null while they load. */
  faculties: FacultyDto[] | null;
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
      {faculties === null && <Skeleton count={PLACEHOLDERS} className="clan-pill-skeleton" />}
      {faculties?.map((f) => {
        const active = selected === f.shortCode;
        return (
          <button
            key={f.id}
            type="button"
            className={`btn btn-sm clan-pill${active ? " active" : ""}`}
            aria-pressed={active}
            title={f.name}
            style={{ ["--clan" as string]: f.color, ["--clan-ink" as string]: readableText(f.color) }}
            onClick={() => onSelect(active ? undefined : f.shortCode)}
          >
            {f.emoji} {f.shortCode}
          </button>
        );
      })}
    </div>
  );
}
