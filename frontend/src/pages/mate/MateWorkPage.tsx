import { useEffect, useMemo, useState } from 'react'
import {
  CheckCircle2,
  Clock3,
  MapPin,
  Pause,
  Play,
  RotateCcw,
  Save,
  X
} from 'lucide-react'
import { useOutletContext } from 'react-router-dom'
import { api } from '../../api/client'
import type {
  Location,
  WorkAssignment,
  WorkSession
} from '../../api/types'
import type { MateShellContext } from '../../mate/MateShell'

function notifySessionChanged() {
  window.dispatchEvent(new Event('mate-session-changed'))
}

function durationSince(startedAt: string) {
  const sec = Math.max(0, Math.floor((Date.now() - new Date(startedAt).getTime()) / 1000))
  const h = Math.floor(sec / 3600)
  const m = Math.floor((sec % 3600) / 60)
  const s = sec % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

export function MateWorkPage() {
  const { currentSession, refreshCurrentSession } = useOutletContext<MateShellContext>()
  const [assignments, setAssignments] = useState<WorkAssignment[]>([])
  const [pending, setPending] = useState(false)
  const [message, setMessage] = useState('')
  const [tick, setTick] = useState(0)
  const [progressTarget, setProgressTarget] = useState<WorkAssignment | null>(null)
  const [pauseTarget, setPauseTarget] = useState<WorkAssignment | null>(null)
  const [completeTarget, setCompleteTarget] = useState<WorkAssignment | null>(null)

  const load = async () => {
    setAssignments(await api.mateAssignments())
  }

  useEffect(() => {
    void load()

    const timer = window.setInterval(() => setTick((x) => x + 1), 1000)
    const source = new EventSource('/api/mate/events', { withCredentials: true })
    source.addEventListener('operation', () => void load())

    return () => {
      window.clearInterval(timer)
      source.close()
    }
  }, [])

  const active = useMemo(
    () => assignments.filter((a) => a.status !== 'COMPLETED' && a.status !== 'CANCELED'),
    [assignments]
  )

  const completed = useMemo(
    () => assignments.filter((a) => a.status === 'COMPLETED').slice(0, 4),
    [assignments]
  )

  const run = async (fn: () => Promise<unknown>, ok: string) => {
    setPending(true)
    setMessage('')
    try {
      await fn()
      setMessage(ok)
      await Promise.all([load(), refreshCurrentSession()])
      notifySessionChanged()
    } catch (e) {
      setMessage(e instanceof Error ? e.message : '처리하지 못했습니다.')
    } finally {
      setPending(false)
    }
  }

  return (
    <div className="mate-page">
      <section className="mate-page-heading">
        <span>MY WORK</span>
        <h1>배정 업무</h1>
        <p>관리자가 지정한 시작점부터 실제 수행한 위치를 기록합니다.</p>
      </section>

      {message && <div className="mate-inline-message">{message}</div>}

      <div className="mate-work-list">
        {active.length === 0 && (
          <div className="mate-empty-card">
            <CheckCircle2 size={28} />
            <strong>진행할 업무가 없습니다.</strong>
          </div>
        )}

        {active.map((assignment) => {
          const isLive = currentSession?.assignmentId === assignment.id

          return (
            <article
              className={`mate-work-card ${isLive ? 'is-live' : ''}`}
              key={assignment.id}
            >
              <header>
                <div>
                  <span className="mate-work-state">{isLive ? 'WORKING' : assignment.status}</span>
                  <small>업무 #{assignment.id}</small>
                </div>
                {isLive && (
                  <div className="mate-live-time">
                    <Clock3 size={14} />
                    {durationSince(currentSession.startedAt)}
                  </div>
                )}
              </header>

              <h2>{assignment.workTypeName}</h2>

              <div className="mate-work-info-grid">
                <div>
                  <span>구역</span>
                  <strong>{assignment.areaLocation}</strong>
                </div>
                <div>
                  <span>시작점</span>
                  <strong>{assignment.startLocation}</strong>
                </div>
              </div>

              <div className="mate-last-location">
                <MapPin size={17} />
                <div>
                  <span>마지막 실제 수행 위치</span>
                  <strong>{assignment.currentLastCompletedLocation ?? '아직 기록 없음'}</strong>
                </div>
              </div>

              {!isLive && assignment.status === 'IN_PROGRESS' && (
                <div className="mate-resume-guide">
                  이전 수행기록은 유지됩니다.
                  {' '}
                  <strong>
                    {assignment.currentLastCompletedLocation
                      ? `${assignment.currentLastCompletedLocation} 이후 업무를 이어서 진행하세요.`
                      : `${assignment.startLocation}부터 이어서 진행하세요.`}
                  </strong>
                </div>
              )}

              <div className="mate-work-actions">
                {assignment.status === 'ASSIGNED' && !isLive && (
                  <button
                    className="mate-primary-button"
                    disabled={pending || Boolean(currentSession)}
                    onClick={() =>
                      void run(() => api.startMateWork(assignment.id), '작업시간 기록을 시작했습니다.')
                    }
                  >
                    <Play size={17} />
                    시작
                  </button>
                )}

                {assignment.status === 'IN_PROGRESS' && !isLive && (
                  <button
                    className="mate-primary-button"
                    disabled={pending || Boolean(currentSession)}
                    onClick={() =>
                      void run(() => api.resumeMateWork(assignment.id), '업무를 재개했습니다.')
                    }
                  >
                    <RotateCcw size={17} />
                    재개
                  </button>
                )}

                {isLive && (
                  <button
                    className="mate-secondary-button"
                    onClick={() => setPauseTarget(assignment)}
                    disabled={pending}
                  >
                    <Pause size={17} />
                    일시정지
                  </button>
                )}

                <button
                  className="mate-secondary-button"
                  onClick={() => setProgressTarget(assignment)}
                  disabled={pending}
                >
                  <Save size={17} />
                  진행기록
                </button>

                <button
                  className="mate-complete-button"
                  onClick={() => setCompleteTarget(assignment)}
                  disabled={pending}
                >
                  <CheckCircle2 size={17} />
                  완료
                </button>
              </div>
            </article>
          )
        })}
      </div>

      {completed.length > 0 && (
        <section className="mate-section mate-completed-section">
          <h2>최근 완료</h2>
          {completed.map((assignment) => (
            <article className="mate-completed-row" key={assignment.id}>
              <div>
                <strong>{assignment.workTypeName}</strong>
                <span>{assignment.areaLocation}</span>
              </div>
              <div className="mate-completed-correction">
                <span>{assignment.currentLastCompletedLocation ?? '-'}</span>
                <button onClick={() => setProgressTarget(assignment)}>
                  기록 정정
                </button>
              </div>
            </article>
          ))}
        </section>
      )}

      {progressTarget && (
        <ProgressSheet
          assignment={progressTarget}
          onClose={() => setProgressTarget(null)}
          onSaved={async () => {
            setProgressTarget(null)
            setMessage('진행 위치를 기록했습니다.')
            await load()
          }}
        />
      )}

      {pauseTarget && (
        <PauseSheet
          assignment={pauseTarget}
          onClose={() => setPauseTarget(null)}
          onPaused={async () => {
            setPauseTarget(null)
            setMessage('작업시간을 일시정지했습니다.')
            await Promise.all([load(), refreshCurrentSession()])
            notifySessionChanged()
          }}
        />
      )}

      {completeTarget && (
        <CompleteSheet
          assignment={completeTarget}
          onClose={() => setCompleteTarget(null)}
          onCompleted={async () => {
            setCompleteTarget(null)
            setMessage('현재 작업을 완료 처리했습니다.')
            await Promise.all([load(), refreshCurrentSession()])
            notifySessionChanged()
          }}
        />
      )}
    </div>
  )
}

function Sheet({
  title,
  onClose,
  children
}: {
  title: string
  onClose: () => void
  children: React.ReactNode
}) {
  return (
    <div className="mate-sheet-backdrop">
      <section className="mate-sheet">
        <header>
          <h2>{title}</h2>
          <button className="mate-icon-button dark" onClick={onClose}>
            <X size={20} />
          </button>
        </header>
        {children}
      </section>
    </div>
  )
}

function ProgressSheet({
  assignment,
  onClose,
  onSaved
}: {
  assignment: WorkAssignment
  onClose: () => void
  onSaved: () => Promise<void>
}) {
  const [locations, setLocations] = useState<Location[]>([])
  const [locationId, setLocationId] = useState('')
  const [reason, setReason] = useState('')
  const [search, setSearch] = useState('')
  const [pending, setPending] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    void api.mateLocations(assignment.areaLocationId).then((data) => {
      const filtered = data.filter(
        (location) => location.fullCode >= assignment.startLocation
      )
      setLocations(filtered)
      const current = filtered.find(
        (location) => location.fullCode === assignment.currentLastCompletedLocation
      )
      if (current) setLocationId(String(current.id))
      else if (filtered.length) setLocationId(String(filtered[0].id))
    })
  }, [assignment.id])

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setPending(true)
    setError('')
    try {
      await api.recordMateProgress(assignment.id, {
        expectedCurrentLocationId:
          assignment.currentLastCompletedLocationId
            ?? undefined,
        lastCompletedLocationId:
          Number(locationId),
        reason: reason || undefined
      })
      await onSaved()
    } catch (e) {
      setError(e instanceof Error ? e.message : '저장하지 못했습니다.')
    } finally {
      setPending(false)
    }
  }

  return (
    <Sheet title="진행기록" onClose={onClose}>
      <form className="mate-sheet-form" onSubmit={submit}>
        <p className="mate-sheet-help">
          실제로 마지막까지 완료한 로케이션 하나만 선택하세요.
          저장 중 관리자가 같은 진행값을 먼저 정정한 경우에는
          오래된 화면 값으로 덮어쓰지 않고 다시 확인하도록 안내합니다.
        </p>

        <label>
          로케이션 검색
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value.toUpperCase())}
            placeholder="예: A01-13"
          />
        </label>

        <label>
          마지막 완료 로케이션
          <select value={locationId} onChange={(e) => setLocationId(e.target.value)} required>
            {locations
              .filter((location) => !search || location.fullCode.includes(search))
              .map((location) => (
                <option value={location.id} key={location.id}>
                  {location.fullCode}
                </option>
              ))}
          </select>
        </label>

        <label>
          정정 사유 <small>선택</small>
          <input
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="오입력 정정 시 입력"
          />
        </label>

        {error && <div className="mate-form-error">{error}</div>}

        <button className="mate-primary-button" disabled={pending || !locationId}>
          저장
        </button>
      </form>
    </Sheet>
  )
}

function PauseSheet({
  assignment,
  onClose,
  onPaused
}: {
  assignment: WorkAssignment
  onClose: () => void
  onPaused: () => Promise<void>
}) {
  const [status, setStatus] = useState('BREAK')
  const [whereabouts, setWhereabouts] = useState('휴게실')
  const [customWhereabouts, setCustomWhereabouts] = useState('')
  const [pending, setPending] = useState(false)

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setPending(true)
    try {
      await api.pauseMateWork(assignment.id, {
        nextStatus: status,
        whereabouts: whereabouts === '기타'
          ? customWhereabouts || '기타'
          : whereabouts
      })
      await onPaused()
    } finally {
      setPending(false)
    }
  }

  return (
    <Sheet title="업무 일시정지" onClose={onClose}>
      <form className="mate-sheet-form" onSubmit={submit}>
        <p className="mate-sheet-help">
          일시정지 중 시간은 실제 작업시간 통계에서 제외됩니다.
        </p>

        <label>
          현재 상태
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="AVAILABLE">대기</option>
            <option value="BREAK">휴게</option>
            <option value="AWAY">자리비움</option>
          </select>
        </label>

        <label>
          거소
          <select
            value={whereabouts}
            onChange={(e) => setWhereabouts(e.target.value)}
          >
            <option>휴게실</option>
            <option>식사</option>
            <option>사무실</option>
            <option>창고</option>
            <option>기타</option>
          </select>
        </label>

        {whereabouts === '기타' && (
          <label>
            기타 거소
            <input
              value={customWhereabouts}
              onChange={(e) => setCustomWhereabouts(e.target.value)}
              placeholder="현재 위치를 입력"
            />
          </label>
        )}

        <button className="mate-primary-button" disabled={pending}>
          일시정지
        </button>
      </form>
    </Sheet>
  )
}

function CompleteSheet({
  assignment,
  onClose,
  onCompleted
}: {
  assignment: WorkAssignment
  onClose: () => void
  onCompleted: () => Promise<void>
}) {
  const [locations, setLocations] = useState<Location[]>([])
  const [locationId, setLocationId] = useState('')
  const [search, setSearch] = useState('')
  const [pending, setPending] = useState(false)

  useEffect(() => {
    void api.mateLocations(assignment.areaLocationId).then((data) => {
      const filtered = data.filter(
        (location) => location.fullCode >= assignment.startLocation
      )
      setLocations(filtered)
      const current = filtered.find(
        (location) => location.fullCode === assignment.currentLastCompletedLocation
      )
      if (current) setLocationId(String(current.id))
    })
  }, [assignment.id])

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setPending(true)
    try {
      await api.completeMateWork(
        assignment.id,
        locationId
          ? {
              expectedCurrentLocationId:
                assignment.currentLastCompletedLocationId
                  ?? undefined,
              lastCompletedLocationId:
                Number(locationId)
            }
          : undefined
      )
      await onCompleted()
    } finally {
      setPending(false)
    }
  }

  return (
    <Sheet title="현재 작업 완료" onClose={onClose}>
      <form className="mate-sheet-form" onSubmit={submit}>
        <div className="mate-warning-box">
          <strong>완료의 의미</strong>
          <p>
            관리자가 정한 전체 구역을 모두 끝냈다는 뜻이 아니라,
            <b> 이번 작업을 현재 위치까지 수행하고 종료</b>한다는 뜻입니다.
          </p>
        </div>

        <label>
          로케이션 검색
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value.toUpperCase())}
            placeholder="예: A01-13"
          />
        </label>

        <label>
          마지막 위치 갱신 <small>선택</small>
          <select value={locationId} onChange={(e) => setLocationId(e.target.value)}>
            <option value="">기존 기록 유지</option>
            {locations
              .filter((location) => !search || location.fullCode.includes(search))
              .map((location) => (
                <option value={location.id} key={location.id}>
                  {location.fullCode}
                </option>
              ))}
          </select>
        </label>

        <button className="mate-complete-button full" disabled={pending}>
          <CheckCircle2 size={18} />
          작업 종료
        </button>
      </form>
    </Sheet>
  )
}
