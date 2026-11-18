import { useEffect, useRef, useState } from "react";
import { isWhatsAppConfigured } from "../lib/whatsapp";
import { WhatsAppReportCTA } from "./WhatsAppReportCTA";

export function WhatsAppFloatingButton() {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    function onMouseDown(e: MouseEvent) {
      if (rootRef.current && !rootRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", onMouseDown);
    return () => document.removeEventListener("mousedown", onMouseDown);
  }, [open]);

  if (!isWhatsAppConfigured()) return null;

  return (
    <div ref={rootRef}>
      {open && (
        <div className="wa-float-popover card">
          <div style={{ fontWeight: 700, marginBottom: 10, textAlign: "center" }}>
            Report litter on WhatsApp
          </div>
          <WhatsAppReportCTA variant="compact" />
        </div>
      )}
      <button
        className="wa-float-btn"
        onClick={() => setOpen((o) => !o)}
        aria-label="Report litter on WhatsApp"
      >
        {open ? "✕" : "💬"}
      </button>
    </div>
  );
}
