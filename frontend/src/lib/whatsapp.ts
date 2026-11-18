export const WHATSAPP_NUMBER = import.meta.env.VITE_WHATSAPP_NUMBER ?? "";
export const WHATSAPP_JOIN_CODE = import.meta.env.VITE_WHATSAPP_JOIN_CODE ?? "";

const DEFAULT_MESSAGE = "Hi! I'd like to report litter in Leuven.";

export function buildWhatsAppLink(message: string = DEFAULT_MESSAGE): string {
  return `https://wa.me/${WHATSAPP_NUMBER}?text=${encodeURIComponent(message)}`;
}

export function isWhatsAppConfigured(): boolean {
  return WHATSAPP_NUMBER.length > 0;
}
