import { lazy, Suspense } from "react";
import { Navigate, Route, Routes, useLocation } from "react-router";
import LandingPage from "./pages/LandingPage";
import { TopBar } from "./components/TopBar";
import { AuthProvider } from "./context/AuthContext";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { Skeleton } from "./components/States";
import { WhatsAppFloatingButton } from "./components/WhatsAppFloatingButton";

// Every page but the landing page loads on demand, so a first visit doesn't
// download Leaflet and the dashboards.
const ProfessionalDashboard = lazy(() => import("./pages/ProfessionalDashboard"));
const StudentDashboard = lazy(() => import("./pages/StudentDashboard"));
const MarketplacePage = lazy(() => import("./pages/MarketplacePage"));
const OperationsPage = lazy(() => import("./pages/OperationsPage"));
const LeaderboardPage = lazy(() => import("./pages/LeaderboardPage"));
const LoginPage = lazy(() => import("./pages/LoginPage"));
const SignupPage = lazy(() => import("./pages/SignupPage"));
const FeedPage = lazy(() => import("./pages/FeedPage"));
const ReportPage = lazy(() => import("./pages/ReportPage"));

const pageLoading = (
  <div className="container" aria-busy="true">
    <Skeleton count={4} />
  </div>
);

// Landing and the report page have their own WhatsApp button; login and signup are forms.
const HIDE_FLOATING_WHATSAPP = ["/", "/report", "/login", "/signup"];

export default function App() {
  const location = useLocation();
  const path = location.pathname;
  const themeClass = path.startsWith("/pro")
    ? "theme-pro"
    : path.startsWith("/student") || path.startsWith("/market")
      ? "theme-student"
      : "";

  return (
    <AuthProvider>
      <div className={`app-shell ${themeClass}`}>
        <TopBar />
        {!HIDE_FLOATING_WHATSAPP.includes(path) && <WhatsAppFloatingButton />}
        <main style={{ flex: 1 }}>
          <Suspense fallback={pageLoading}>
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
          </Suspense>
        </main>
        <footer className="app-footer">
          © 2026 ReCode
        </footer>
      </div>
    </AuthProvider>
  );
}
