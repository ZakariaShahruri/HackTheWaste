import type { ReactNode } from "react";

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div role="alert" className="state state-error">
      <div className="state-title">Couldn't load this</div>
      <div className="state-detail">{message}</div>
      {onRetry && (
        <button type="button" className="btn btn-ghost btn-sm" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  );
}

export function EmptyState({ icon, title, children }: { icon?: string; title: string; children?: ReactNode }) {
  return (
    <div className="state">
      {icon && <div className="state-icon" aria-hidden>{icon}</div>}
      <div className="state-title">{title}</div>
      {children && <div className="state-detail">{children}</div>}
    </div>
  );
}

/** Grey shimmering placeholders, shaped by `className`, while the first result loads. */
export function Skeleton({ count = 3, className = "skeleton-row" }: { count?: number; className?: string }) {
  return (
    <>
      {Array.from({ length: count }, (_, i) => (
        <div key={i} className={`skeleton ${className}`} aria-hidden />
      ))}
    </>
  );
}
