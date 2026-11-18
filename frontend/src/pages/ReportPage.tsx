import { WhatsAppReportCTA } from "../components/WhatsAppReportCTA";

const STEPS = [
  {
    icon: "📍",
    title: "Send your location",
    desc: "Share a location pin in the chat — just where you're standing.",
  },
  {
    icon: "📸",
    title: "Send a photo",
    desc: "Snap the litter. Our AI checks it's a genuine on-site photo.",
  },
  {
    icon: "🤖",
    title: "Get an instant score",
    desc: "Gemini AI rates the cleanliness 0–100 and tags what it sees, in seconds.",
  },
  {
    icon: "🏆",
    title: "Score points for your clan",
    desc: "Your report goes live on the Leuven Go feed and credits your faculty.",
  },
];

export default function ReportPage() {
  return (
    <div className="container" style={{ maxWidth: 720 }}>
      <section className="landing-hero" style={{ padding: "24px 0 8px" }}>
        <span className="landing-eyebrow">Report litter</span>
        <h1>Report on WhatsApp</h1>
        <p>
          No app to download, no form to fill in — just chat with our bot and
          get an instant AI cleanliness score.
        </p>
      </section>

      <section className="card" style={{ maxWidth: 420, margin: "24px auto" }}>
        <WhatsAppReportCTA variant="full" />
      </section>

      <section style={{ marginTop: 32 }}>
        <h2 style={{ fontSize: 20, marginBottom: 16 }}>How it works</h2>
        <div className="stack">
          {STEPS.map((s, i) => (
            <div
              key={s.title}
              className="card"
              style={{ display: "flex", gap: 14, alignItems: "flex-start" }}
            >
              <div style={{ fontSize: 24 }}>{s.icon}</div>
              <div>
                <div style={{ fontWeight: 700 }}>
                  {i + 1}. {s.title}
                </div>
                <div className="muted" style={{ fontSize: 14, marginTop: 2 }}>
                  {s.desc}
                </div>
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
