import { QRCodeSVG } from "qrcode.react";
import {
  WHATSAPP_JOIN_CODE,
  buildWhatsAppLink,
  isWhatsAppConfigured,
} from "../lib/whatsapp";

interface Props {
  variant: "full" | "compact" | "inline";
  message?: string;
  className?: string;
}

export function WhatsAppReportCTA({ variant, message, className }: Props) {
  if (!isWhatsAppConfigured()) return null;

  const href = buildWhatsAppLink(message);

  const button = (
    <a
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      className="btn btn-whatsapp"
    >
      💬 Report on WhatsApp
    </a>
  );

  if (variant === "inline") {
    return <div className={className}>{button}</div>;
  }

  const qrSize = variant === "full" ? 180 : 96;

  return (
    <div
      className={className}
      style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 14 }}
    >
      {button}
      <div style={{ background: "white", padding: 10, borderRadius: 12 }}>
        <QRCodeSVG value={href} size={qrSize} title="QR code that opens our WhatsApp chat" />
      </div>
      {variant === "full" && WHATSAPP_JOIN_CODE && (
        <span className="tag tag-info" style={{ textAlign: "center" }}>
          Not connected yet? Text "join {WHATSAPP_JOIN_CODE}" to our WhatsApp number first.
        </span>
      )}
    </div>
  );
}
