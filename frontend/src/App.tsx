import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { useAuth } from './auth/AuthContext'
import { AppShell } from './components/AppShell'
import { MateShell } from './mate/MateShell'
import { AssignmentsPage } from './pages/AssignmentsPage'
import { ActionQueuePage } from './pages/ActionQueuePage'
import { AuditLogsPage } from './pages/AuditLogsPage'
import { DashboardPage } from './pages/DashboardPage'
import { IssuesPage } from './pages/IssuesPage'
import { IntegrityPage } from './pages/IntegrityPage'
import { ShiftClosePage } from './pages/ShiftClosePage'
import { HandoverPage } from './pages/HandoverPage'
import { HandoverOverviewPage } from './pages/HandoverOverviewPage'
import { LoginPage } from './pages/LoginPage'
import { MatesPage } from './pages/MatesPage'
import { NoticesPage } from './pages/NoticesPage'
import { OperationsPage } from './pages/OperationsPage'
import { ReportsPage } from './pages/ReportsPage'
import { SettingsPage } from './pages/SettingsPage'
import { SystemReadinessPage } from './pages/SystemReadinessPage'
import { MateHomePage } from './pages/mate/MateHomePage'
import { MateIssuesPage } from './pages/mate/MateIssuesPage'
import { MateLoginPage } from './pages/mate/MateLoginPage'
import { MateMorePage } from './pages/mate/MateMorePage'
import { MateNoticesPage } from './pages/mate/MateNoticesPage'
import { MateWorkPage } from './pages/mate/MateWorkPage'

function RequireAdmin() {
  const { user, loading } = useAuth()
  const location = useLocation()

  if (loading) return <div className="boot-screen">시스템 확인 중…</div>

  if (user?.role === 'MATE') {
    return <Navigate to="/mate" replace />
  }

  if (!user || user.role !== 'ADMIN') {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  return <AppShell />
}

function RequireMate() {
  const { user, loading } = useAuth()
  const location = useLocation()

  if (loading) return <div className="mate-boot-screen">근무정보 확인 중…</div>

  if (user?.role === 'ADMIN') {
    return <Navigate to="/" replace />
  }

  if (!user || user.role !== 'MATE') {
    return (
      <Navigate
        to="/mate/login"
        replace
        state={{ from: location.pathname }}
      />
    )
  }

  return <MateShell />
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/mate/login" element={<MateLoginPage />} />

      <Route element={<RequireAdmin />}>
        <Route index element={<DashboardPage />} />
        <Route path="/operations" element={<OperationsPage />} />
        <Route path="/action-queue" element={<ActionQueuePage />} />
        <Route path="/integrity" element={<IntegrityPage />} />
        <Route path="/handover" element={<HandoverPage />} />
        <Route path="/handover-overview" element={<HandoverOverviewPage />} />
        <Route path="/shift-close" element={<ShiftClosePage />} />
        <Route path="/assignments" element={<AssignmentsPage />} />
        <Route path="/audit-logs" element={<AuditLogsPage />} />
        <Route path="/readiness" element={<SystemReadinessPage />} />
        <Route path="/issues" element={<IssuesPage />} />
        <Route path="/notices" element={<NoticesPage />} />
        <Route path="/mates" element={<MatesPage />} />
        <Route path="/reports" element={<ReportsPage />} />
        <Route path="/settings" element={<SettingsPage />} />
      </Route>

      <Route path="/mate" element={<RequireMate />}>
        <Route index element={<MateHomePage />} />
        <Route path="work" element={<MateWorkPage />} />
        <Route path="issues" element={<MateIssuesPage />} />
        <Route path="notices" element={<MateNoticesPage />} />
        <Route path="more" element={<MateMorePage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
