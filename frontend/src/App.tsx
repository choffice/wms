import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { useAuth } from './auth/AuthContext'
import { AppShell } from './components/AppShell'
import { MateShell } from './mate/MateShell'
import { DashboardPage } from './pages/DashboardPage'
import { LoginPage } from './pages/LoginPage'
import { NoticesPage } from './pages/NoticesPage'
import { SimpleAssignmentsPage } from './pages/SimpleAssignmentsPage'
import { SimpleIssuesPage } from './pages/SimpleIssuesPage'
import { SimpleSettingsPage } from './pages/SimpleSettingsPage'
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
        <Route path="/assignments" element={<SimpleAssignmentsPage />} />
        <Route path="/issues" element={<SimpleIssuesPage />} />
        <Route path="/notices" element={<NoticesPage />} />
        <Route path="/settings" element={<SimpleSettingsPage />} />
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
