import {
  Bell,
  ClipboardList,
  Home,
  LogOut,
  Menu,
  TriangleAlert
} from 'lucide-react'
import { NavLink, Outlet } from 'react-router-dom'
import { useCallback, useEffect, useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import { api } from '../api/client'
import type { WorkSession } from '../api/types'
import { useOnlineStatus } from './useOnlineStatus'

export type MateShellContext = {
  currentSession: WorkSession | null
  refreshCurrentSession: () => Promise<void>
}

export function MateShell() {
  const { user, logout, refresh } = useAuth()
  const online = useOnlineStatus()
  const [currentSession, setCurrentSession] = useState<WorkSession | null>(null)

  const refreshCurrentSession = useCallback(async () => {
    try {
      setCurrentSession(await api.currentMateSession())
    } catch {
      setCurrentSession(null)
    }
  }, [])

  useEffect(() => {
    void refreshCurrentSession()

    const handler = () => void refreshCurrentSession()
    window.addEventListener('mate-session-changed', handler)

    const poll = window.setInterval(() => {
      void refreshCurrentSession()
    }, 30000)

    return () => {
      window.removeEventListener('mate-session-changed', handler)
      window.clearInterval(poll)
    }
  }, [refreshCurrentSession])

  useEffect(() => {
    const source = new EventSource('/api/mate/events', {
      withCredentials: true
    })

    source.addEventListener('operation', () => {
      void Promise.all([
        refresh(),
        refreshCurrentSession()
      ])
    })

    return () => source.close()
  }, [refresh, refreshCurrentSession])

  useEffect(() => {
    if (!currentSession) return

    const send = () => {
      void api.heartbeat(currentSession.id).catch(() => {
        // 다음 poll에서 서버 상태를 다시 확인한다.
      })
    }

    send()
    const timer = window.setInterval(send, 60000)
    return () => window.clearInterval(timer)
  }, [currentSession?.id])

  return (
    <div className="mate-app">
      <header className="mate-topbar">
        <div>
          <span className="mate-app-label">MATE WORK</span>
          <strong>{user?.nickname ?? user?.employeeNo}</strong>
        </div>

        <div className="mate-topbar-right">
          <div className="mate-pda-pill">
            PDA {user?.pdaNumber ?? '-'}
          </div>
          <button
            className="mate-icon-button"
            aria-label="로그아웃"
            onClick={() => void logout()}
          >
            <LogOut size={18} />
          </button>
        </div>
      </header>

      {!online && (
        <div className="mate-offline-strip">
          오프라인 상태 · 업무 기록 저장은 네트워크 연결 후 가능합니다.
        </div>
      )}

      {currentSession && (
        <div className="mate-active-strip">
          <span className="pulse-dot" />
          작업시간 기록 중
          <small>세션 #{currentSession.id}</small>
        </div>
      )}

      <main className="mate-main">
        <Outlet context={{ currentSession, refreshCurrentSession } satisfies MateShellContext} />
      </main>

      <nav className="mate-bottom-nav">
        <NavLink to="/mate" end>
          <Home size={20} />
          <span>홈</span>
        </NavLink>
        <NavLink to="/mate/work">
          <ClipboardList size={20} />
          <span>업무</span>
        </NavLink>
        <NavLink to="/mate/issues">
          <TriangleAlert size={20} />
          <span>특이사항</span>
        </NavLink>
        <NavLink to="/mate/notices">
          <Bell size={20} />
          <span>공지</span>
        </NavLink>
        <NavLink to="/mate/more">
          <Menu size={20} />
          <span>더보기</span>
        </NavLink>
      </nav>
    </div>
  )
}
