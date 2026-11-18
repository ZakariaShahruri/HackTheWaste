import { Navigate, Route, Routes, useLocation } from "react-router";
import LandingPage from "./pages/LandingPage";
import ProfessionalDashboard from "./pages/ProfessionalDashboard";
import StudentDashboard from "./pages/StudentDashboard";
import MarketplacePage from "./pages/MarketplacePage";
import OperationsPage from "./pages/OperationsPage";
import LeaderboardPage from "./pages/LeaderboardPage";
import LoginPage from "./pages/LoginPage";
import SignupPage from "./pages/SignupPage";
import FeedPage from "./pages/FeedPage";
import ReportPage from "./pages/ReportPage";
import { TopBar } from "./components/TopBar";
import { AuthProvider } from "./context/AuthContext";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { WhatsAppFloatingButton } from "./components/WhatsAppFloatingButton";

const HIDE_FLOATING_WHATSAPP = ["/report", "/login", "/signup"];

export default function App() {
  const location = useLocation();
  const path = location.pathname;
  const themeClass = path.startsWith("/pro")
    ? "theme-pro"
    : path.startsWith("/student") ||
path.startsWith("/market") ||
        path.startsWith("/leaderboard")
      ? "theme-student"
      : "";

  return (
    <AuthProvider>
      <div className={`app-shell ${themeClass}`}>
        <TopBar />
        {!HIDE_FLOATING_WHATSAPP.includes(path) && <WhatsAppFloatingButton />}
        <main style={{ flex: 1 }}>
          <Routes>
            <Route path="/" element={<LandingPage />} />
            <Route path="/pro" element={<ProfessionalDashboard />} />
            <Route path="/pro/operations" element={<ProtectedRoute role="CREW"><OperationsPage /></ProtectedRoute>} />
            <Route path="/student" element={<StudentDashboard />} />
            <Route path="/student/leaderboard" element={<LeaderboardPage />} />
            <Route path="/market" element={<MarketplacePage />} />
            <Route path="/feed" element={<FeedPage />} />
            <Route path="/report" element={<ReportPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/signup" element={<SignupPage />} />
            <Route path="*" element={<Navigate to="/" />} />
          </Routes>
        </main>
        <footer className="app-footer">
          © 2026 ReCode
        </footer>
      </div>
    </AuthProvider>
  );
}
