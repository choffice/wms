import {
  Activity,
  AlertTriangle,
  BarChart3,
  Bell,
  ClipboardList,
  CheckCircle2,
  FileClock,
  Gauge,
  ArrowRightLeft,
  LayoutDashboard,
  LogOut,
  Megaphone,
  Settings,
  ShieldCheck,
  UsersRound
} from 'lucide-react'
import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

const nav = [
  { to: '/', label: '현황판', icon: LayoutDashboard },
  { to: '/operations', label: '운영관제', icon: Activity },
  { to: '/action-queue', label: '후속조치', icon: AlertTriangle },
  { to: '/handover', label: '인수인계', icon: ArrowRightLeft },
  { to: '/handover-overview', label: '인계요약', icon: FileClock },
  { to: '/shift-close', label: '마감점검', icon: CheckCircle2 },
  { to: '/integrity', label: '정합성', icon: ShieldCheck },
  { to: '/assignments', label: '업무배정', icon: ClipboardList },
  { to: '/issues', label: '특이사항', icon: AlertTriangle },
  { to: '/notices', label: '공지사항', icon: Megaphone },
  { to: '/mates', label: 'MATE', icon: UsersRound },
  { to: '/reports', label: '보고서', icon: BarChart3 },
  { to: '/audit-logs', label: '감사로그', icon: FileClock },
  { to: '/readiness', label: '시연점검', icon: Gauge },
  { to: '/settings', label: '설정', icon: Settings }
]

export function AppShell() {
  const { user, logout } = useAuth()

  return (
    <div className="app-shell">
      <aside className="side-nav">
        <div className="brand">
          <span className="brand-mark">W</span>
          <div><strong>WAREHOUSE</strong><small>CONTROL SYSTEM</small></div>
        </div>

        <nav>
          {nav.map(({ to, label, icon: Icon }) => (
            <NavLink key={to} to={to} end={to === '/'} className={({ isActive }) => (isActive ? 'nav-item active' : 'nav-item')}>
              <Icon size={18}/><span>{label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="side-bottom">
          <div className="admin-card"><Bell size={17}/><div><small>ADMIN</small><strong>{user?.employeeNo}</strong></div></div>
          <button className="ghost-button" onClick={() => void logout()}><LogOut size={16}/>로그아웃</button>
        </div>
      </aside>

      <main className="main-column">
        <header className="topbar">
          <div><h1>물류 운영 관리 시스템</h1><p>업무·로케이션·MATE·PDA 통합 현황</p></div>
          <div className="server-pill"><span className="dot online"/>API ONLINE</div>
        </header>
        <div className="page-container"><Outlet/></div>
      </main>
    </div>
  )
}
