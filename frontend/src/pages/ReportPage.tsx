import { useSearchParams } from "react-router";
import { WhatsAppReportCTA } from "../components/WhatsAppReportCTA";
import { BrowserReport } from "../components/report/BrowserReport";
import { isWhatsAppConfigured } from "../lib/whatsapp";

const STEPS = [
  { icon: "📍", title: "Send your location", desc: "Share a location pin in the chat — just where you're standing." },
  { icon: "📸", title: "Send a photo", desc: "Snap the litter. Our AI checks it's a genuine on-site photo." },
  { icon: "🤖", title: "Get an instant score", desc: "Gemini rates the cleanliness 0–100 and tags what it sees, in seconds." },
  { icon: "🏆", title: "Score points for your clan", desc: "Text “clan CODE” once, and every report credits your faculty." },
];

type Channel = "whatsapp" | "browser";

export default function ReportPage() {
  const [params, setParams] = useSearchParams();
  const whatsApp = isWhatsAppConfigured();
  const channel: Channel = !whatsApp || params.get("via") === "browser" ? "browser" : "whatsapp";

  return (
    <div className="container report-page">
      <section className="landing-hero report-hero">
        <span className="landing-eyebrow">Report litter</span>
        <h1>{channel === "whatsapp" ? "Report on WhatsApp" : "Report from your browser"}</h1>
        <p>
          A location and a photo is all it takes. The AI scores the spot, and three reports in one place send a
          cleaning crew.
        </p>
      </section>

      {whatsApp && (
        <div className="tabbar report-tabs" role="tablist" aria-label="How to report">
          <ChannelTab channel="whatsapp" current={channel} onSelect={() => setParams({}, { replace: true })}>
            💬 WhatsApp
          </ChannelTab>
          <ChannelTab channel="browser" current={channel} onSelect={() => setParams({ via: "browser" }, { replace: true })}>
            🌐 In your browser
          </ChannelTab>
        </div>
      )}

      <div role={whatsApp ? "tabpanel" : undefined} id={`report-${channel}`}>
        {channel === "whatsapp" ? <WhatsAppSteps /> : <section className="card"><BrowserReport /></section>}
      </div>
    </div>
  );
}

function ChannelTab({
  channel,
  current,
  onSelect,
  children,
}: {
  channel: Channel;
  current: Channel;
  onSelect: () => void;
  children: React.ReactNode;
}) {
  const active = channel === current;
  return (
    <button
      type="button"
      role="tab"
      aria-selected={active}
      aria-controls={`report-${channel}`}
      className={active ? "active" : ""}
      onClick={onSelect}
    >
      {children}
    </button>
  );
}

function WhatsAppSteps() {
  return (
    <>
      <section className="card report-whatsapp">
        <WhatsAppReportCTA variant="full" />
      </section>

      <section className="section">
        <h2 className="report-how">How it works</h2>
        <ol className="stack report-how-steps">
          {STEPS.map((s) => (
            <li key={s.title} className="card report-how-step">
              <div className="report-how-icon" aria-hidden>{s.icon}</div>
              <div>
                <div className="report-how-title">{s.title}</div>
                <div className="muted">{s.desc}</div>
              </div>
            </li>
          ))}
        </ol>
      </section>
    </>
  );
}
