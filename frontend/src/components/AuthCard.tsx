import type { ReactNode } from "react";

interface Props {
  title: string;
  subtitle: string;
  /** The line under the card, e.g. a link to the other auth page. */
  footer: ReactNode;
  wide?: boolean;
  children: ReactNode;
}

export function AuthCard({ title, subtitle, footer, wide, children }: Props) {
  return (
    <div className={`container auth-page${wide ? " auth-page-wide" : ""}`}>
      <div className="card auth-card">
        <h1 className="auth-title">{title}</h1>
        <p className="muted auth-subtitle">{subtitle}</p>
        {children}
        <p className="auth-switch">{footer}</p>
      </div>
    </div>
  );
}
