import { useEffect, useMemo, useRef, useState } from 'react'
import { AlertTriangle, ClipboardCheck, RefreshCw, UsersRound } from 'lucide-react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import type { AdminDashboard } from '../api/types'
import { Panel } from '../components/Panel'
import { StatusBadge } from '../components/StatusBadge'

function compact(text: string, max = 74) {
  return text.length > max ? `${text.slice(0, max)}…` : text
}

function time(value: string | null) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

export function DashboardPage() {
  const [data, setData] = useState<AdminDashboard | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [hiddenLogIds, setHiddenLogIds] = useState<number[]>([])
  const reloadTimer = useRef<number | null>(null)

  const load = async () => {
    setLoading(true)
    setError('')
    try {
      setData(await api.dashboard())
    } catch (e) {
      setError(e instanceof Error ? e.message : '현황을 불러오지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()

    const events = new EventSource('/api/admin/events', { withCredentials: true })
    events.addEventListener('operation', () => {
      if (reloadTimer.current) window.clearTimeout(reloadTimer.current)
      reloadTimer.current = window.setTimeout(() => void load(), 250)
    })

    return () => {
      events.close()
      if (reloadTimer.current) window.clearTimeout(reloadTimer.current)
    }
  }, [])

  const workingCount = useMemo(
    () => data?.mates.filter((m) => m.status === 'WORKING').length ?? 0,
    [data]
  )

  const visibleLogs = useMemo(
    () => data?.latestLogs.filter((log) => !hiddenLogIds.includes(log.id)) ?? [],
    [data, hiddenLogIds]
  )

  if (loading && !data) {
    return <div className="loading-state">현황 데이터를 불러오는 중입니다…</div>
  }

  if (!data) {
    return (
      <div className="error-state">
        <strong>백엔드 연결을 확인해주세요.</strong>
        <span>{error}</span>
        <button className="secondary-button" onClick={() => void load()}>
          다시 시도
        </button>
      </div>
    )
  }

  return (
    <div className="dashboard-grid">
      <div className="page-title-row full-span">
        <div>
          <span className="eyebrow">TODAY</span>
          <h2>오늘 현장 현황</h2>
          <p>근무자, 배정업무, 진행위치와 특이사항만 빠르게 확인합니다.</p>
        </div>
        <div className="erp-cell-actions">
          <Link className="primary-button compact" to="/assignments">업무배정</Link>
          <Link className="secondary-button compact" to="/issues">특이사항 전체보기</Link>
          <Link className="secondary-button compact" to="/settings?tab=mate">근무스케줄</Link>
        </div>
      </div>

      <div className="metric-row full-span">
        <div className="metric-card">
          <UsersRound size={19} />
          <div><strong>{data.mates.length}</strong><span>MATE</span></div>
        </div>
        <div className="metric-card">
          <ClipboardCheck size={19} />
          <div><strong>{workingCount}</strong><span>현재 작업중</span></div>
        </div>
        <div className="metric-card">
          <AlertTriangle size={19} />
          <div><strong>{data.issues.length}</strong><span>미확인 특이사항</span></div>
        </div>
        <button className="refresh-card" onClick={() => void load()}>
          <RefreshCw size={18} />
          새로고침
        </button>
      </div>

      <Panel
        title="공지사항"
        className="notice-panel"
        action={<Link className="erp-row-button" to="/notices">전체 편집</Link>}
      >
        <div className="erp-notice-list">
          {data.notices.length === 0 && <div className="empty">표시 중인 공지사항이 없습니다.</div>}
          {data.notices.slice(0, 6).map((notice) => (
            <article className={`erp-notice-row ${notice.important ? 'important' : ''}`} key={notice.id}>
              <span className="erp-notice-flag">{notice.important ? '중요' : '일반'}</span>
              <p>{notice.content}</p>
              <time>{time(notice.updatedAt)}</time>
            </article>
          ))}
        </div>
      </Panel>

      <Panel
        title="특이사항"
        className="issue-panel"
        action={<Link className="erp-row-button" to="/issues">전체보기</Link>}
      >
        <div className="issue-list">
          {data.issues.length === 0 && <div className="empty">미확인 특이사항이 없습니다.</div>}
          {data.issues.slice(0, 6).map((issue) => (
            <article className="issue-row" key={issue.id}>
              <div>
                <div className="row-kicker">
                  {issue.isNew && <span className="new-badge">NEW</span>}
                  <strong>{issue.issueType}</strong>
                  <span>{issue.authorNickname}</span>
                </div>
                <p>{compact(issue.comment)}</p>
              </div>
              <div className="row-meta">
                <span>{issue.location ?? '-'}</span>
                <time>{time(issue.createdAt)}</time>
              </div>
            </article>
          ))}
        </div>
      </Panel>

      <Panel
        title="MATE 현황판"
        className="mate-panel"
        action={<Link className="erp-row-button" to="/assignments">업무배정</Link>}
      >
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>MATE</th>
                <th>PDA</th>
                <th>상태</th>
                <th>배정업무</th>
                <th>배정구역</th>
                <th>마지막 진행위치</th>
              </tr>
            </thead>
            <tbody>
              {data.mates.map((mate) => (
                <tr key={mate.mateId}>
                  <td><strong>{mate.nickname}</strong></td>
                  <td>{mate.pdaNumber ?? '-'}</td>
                  <td><StatusBadge status={mate.status} /></td>
                  <td>{mate.workType ?? '-'}</td>
                  <td>{mate.area ?? '-'}</td>
                  <td>{mate.lastCompletedLocation ?? '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Panel>

      <Panel title="업무 진행도" className="location-panel">
        <div className="table-wrap">
          <table className="erp-location-table">
            <thead>
              <tr>
                <th>구역</th>
                <th>업무</th>
                <th>진행위치</th>
                <th>위치 기준 진행도</th>
                <th>최근 수행자</th>
                <th>보고시각</th>
              </tr>
            </thead>
            <tbody>
              {data.areaWorkStatuses.length === 0 && (
                <tr><td colSpan={6} className="empty-cell">로케이션 또는 업무 종류를 먼저 등록해주세요.</td></tr>
              )}
              {data.areaWorkStatuses.slice(0, 24).map((row) => (
                <tr key={`${row.areaId}-${row.workTypeId}`}>
                  <td><strong>{row.areaCode}</strong></td>
                  <td>{row.workType}</td>
                  <td>{row.lastCompletedLocation ?? '미실시'}</td>
                  <td>
                    <div className="erp-progress-cell">
                      <div className="progress-track"><span style={{ width: `${row.progressPercent}%` }} /></div>
                      <b>{row.progressPercent}%</b>
                    </div>
                  </td>
                  <td>{row.lastMateNickname ?? '-'}</td>
                  <td>{row.lastPerformedAt ? time(row.lastPerformedAt) : '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Panel>

      <Panel
        title="최근 작업 로그"
        className="log-panel"
        action={visibleLogs.length > 0 ? (
          <button
            className="secondary-button compact"
            onClick={() => setHiddenLogIds(visibleLogs.map((log) => log.id))}
          >
            현재 표시 지우기
          </button>
        ) : undefined}
      >
        <div className="log-list">
          {visibleLogs.length === 0 && <div className="empty">표시할 로그가 없습니다.</div>}
          {visibleLogs.slice(0, 10).map((log) => (
            <div className="log-row" key={log.id}>
              <time>{time(log.createdAt)}</time>
              <span className="log-type">[{log.type}]</span>
              <span>{log.actor ?? '-'} → {log.target ?? '-'}</span>
              <strong>{log.message}</strong>
            </div>
          ))}
        </div>
      </Panel>
    </div>
  )
}
