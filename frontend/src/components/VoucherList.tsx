import type { RedemptionDto } from "../api/types";

/** The rewards a user has redeemed, with the codes to show at the counter. */
export function VoucherList({ redemptions }: { redemptions: RedemptionDto[] }) {
  return (
    <ul className="voucher-list">
      {redemptions.map((r) => (
        <li key={r.id} className="voucher">
          <div>
            <div style={{ fontWeight: 700 }}>{r.title}</div>
            <div className="muted" style={{ fontSize: 12 }}>
              {r.sponsor} · {r.costPoints} pts · {new Date(r.redeemedAt).toLocaleDateString("nl-BE")}
            </div>
          </div>
          <code className="voucher-code">{r.voucherCode}</code>
        </li>
      ))}
    </ul>
  );
}
