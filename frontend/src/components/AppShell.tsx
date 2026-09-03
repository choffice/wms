import {
  LayoutDashboard,
  LogOut,
  Megaphone,
  Settings
} from 'lucide-react'
import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

const nav = [
  { to: '/', label: '현황판', icon: LayoutDashboard },
  { to: '/notices', label: '공지사항', icon: Megaphone },
  { to: '/settings', label: '설정', icon: Settings }
]

export function AppShell() {
  const { user, logout } = useAuth()

  return (
    <div className="app-shell">
      <aside className="side-nav">
        <div className="brand">
          <span className="brand-mark">W</span>
          <div><strong>WAREHOUSE</strong><small>WORK MANAGEMENT</small></div>
        </div>

        <nav>
          {nav.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/'}
              className={({ isActive }) => (isActive ? 'nav-item active' : 'nav-item')}
            >
              <Icon size={18}/><span>{label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="side-bottom">
          <div className="admin-card">
            <div><small>ADMIN</small><strong>{user?.employeeNo}</strong></div>
          </div>
          <button className="ghost-button" onClick={() => void logout()}>
            <LogOut size={16}/>로그아웃
          </button>
        </div>
      </aside>

      <main className="main-column">
        <header className="topbar">
          <div>
            <h1>물류센터 근무·업무 관리</h1>
            <p>MATE · 근무스케줄 · 업무배정 · 진행위치 · 특이사항</p>
          </div>
          <div className="server-pill"><span className="dot online"/>API ONLINE</div>
        </header>
        <div className="page-container"><Outlet/></div>
      </main>
    </div>
  )
}
