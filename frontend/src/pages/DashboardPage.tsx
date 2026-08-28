import { useEffect, useMemo, useRef, useState } from 'react'
import { AlertTriangle, ClipboardCheck, RefreshCw, UsersRound } from 'lucide-react'
import { api } from '../api/client'
import type { AdminDashboard } from '../api/types'
import { Panel } from '../components/Panel'
import { StatusBadge } from '../components/StatusBadge'

function compact(text: string, max = 74) {
  return text.length > max ? `${text.slice(0, max)}…` : text
}

function eta(value: number | null) {
  if (value == null) return '이력 부족'
  const rawMinutes = Math.max(0, Math.round(value / 60))
  const rounded = rawMinutes < 10 ? rawMinutes : Math.round(rawMinutes / 10) * 10
  const hours = Math.floor(rounded / 60)
  const minutes = rounded % 60
  if (hours === 0) return `약 ${minutes}분`
  if (minutes === 0) return `약 ${hours}시간`
  return `약 ${hours}시간 ${minutes}분`
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
    () => data?.latestLogs.filter(
      (log) => !hiddenLogIds.includes(log.id)
    ) ?? [],
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
      <div className="metric-row full-span">
        <div className="metric-card">
          <UsersRound size={19} />
          <div><strong>{data.mates.length}</strong><span>활성 MATE</span></div>
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
          현황 새로고침
        </button>
      </div>

      <Panel title="공지사항" className="notice-panel">
        <div className="erp-notice-list">
          {data.notices.length === 0 && <div className="empty">표시 중인 공지사항이 없습니다.</div>}
          {data.notices.slice(0, 8).map((notice) => (
            <article className={`erp-notice-row ${notice.important ? 'important' : ''}`} key={notice.id}>
              <span className="erp-notice-flag">{notice.important ? '중요' : '일반'}</span>
              <p>{notice.content}</p>
              <time>{time(notice.updatedAt)}</time>
            </article>
          ))}
        </div>
      </Panel>

      <Panel title="특이사항" className="issue-panel">
        <div className="issue-list">
          {data.issues.length === 0 && <div className="empty">미확인 특이사항이 없습니다.</div>}
          {data.issues.slice(0, 8).map((issue) => (
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

      <Panel title="MATE 현황판" className="mate-panel">
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>MATE</th>
                <th>PDA</th>
                <th>상태</th>
                <th>배정업무</th>
                <th>구역 / 진행</th>
              </tr>
            </thead>
            <tbody>
              {data.mates.map((mate) => (
                <tr key={mate.mateId}>
                  <td><strong>{mate.nickname}</strong></td>
                  <td>{mate.pdaNumber ?? '-'}</td>
                  <td><StatusBadge status={mate.status} /></td>
                  <td>{mate.workType ?? '-'}</td>
                  <td>
                    <span className="location-inline">{mate.area ?? '-'}</span>
                    {mate.lastCompletedLocation && (
                      <small> · {mate.lastCompletedLocation}</small>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Panel>

      <Panel title="로케이션 업무 현황" className="location-panel">
        <div className="table-wrap">
          <table className="erp-location-table">
            <thead>
              <tr>
                <th>구역</th>
                <th>업무</th>
                <th>구역 위치</th>
                <th>최종 위치</th>
                <th>예상 잔여</th>
                <th>표본</th>
                <th>최종 수행자</th>
                <th>최종 수행시각</th>
              </tr>
            </thead>
            <tbody>
              {data.areaWorkStatuses.length === 0 && (
                <tr><td colSpan={8} className="empty-cell">로케이션 또는 업무 종류를 먼저 등록해주세요.</td></tr>
              )}
              {data.areaWorkStatuses.slice(0, 24).map((row) => (
                <tr key={`${row.areaId}-${row.workTypeId}`}>
                  <td><strong>{row.areaCode}</strong></td>
                  <td>{row.workType}</td>
                  <td>
                    <div className="erp-progress-cell">
                      <div className="progress-track"><span style={{ width: `${row.progressPercent}%` }} /></div>
                      <b>{row.progressPercent}%</b>
                    </div>
                  </td>
                  <td>{row.lastCompletedLocation ?? '미실시'}</td>
                  <td>
                    <strong className="erp-eta-value">
                      {eta(row.estimatedRemainingSeconds)}
                    </strong>
                  </td>
                  <td>{row.estimateSampleCount > 0 ? `${row.estimateSampleCount}건` : '-'}</td>
                  <td>{row.lastMateNickname ?? '-'}</td>
                  <td>{row.lastPerformedAt ? time(row.lastPerformedAt) : '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Panel>

      <Panel
        title="사용 로그"
        className="log-panel"
        action={visibleLogs.length > 0 ? (
          <button
            className="secondary-button compact"
            onClick={() =>
              setHiddenLogIds(
                visibleLogs.map((log) => log.id)
              )
            }
          >
            현재 표시 숨기기
          </button>
        ) : undefined}
      >
        <div className="log-list">
          {visibleLogs.length === 0 && <div className="empty">표시할 로그가 없습니다.</div>}
          {visibleLogs.map((log) => (
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
