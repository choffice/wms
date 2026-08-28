import { useEffect, useMemo, useState } from 'react'
import {
  ArrowRight,
  BellRing,
  Clock3,
  MapPin,
  PackageCheck,
  Smartphone
} from 'lucide-react'
import { Link, useOutletContext } from 'react-router-dom'
import { api } from '../../api/client'
import type {
  Notice,
  PdaUsage,
  WorkAssignment
} from '../../api/types'
import type { MateShellContext } from '../../mate/MateShell'

function dateTime(value: string | null) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('ko-KR', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

export function MateHomePage() {
  const { currentSession } = useOutletContext<MateShellContext>()
  const [assignments, setAssignments] = useState<WorkAssignment[]>([])
  const [notices, setNotices] = useState<Notice[]>([])
  const [pda, setPda] = useState<PdaUsage | null>(null)
  const [loading, setLoading] = useState(true)

  const load = async () => {
    setLoading(true)
    try {
      const [work, notice, currentPda] = await Promise.all([
        api.mateAssignments(),
        api.mateNotices(),
        api.currentPdaUsage()
      ])
      setAssignments(work)
      setNotices(notice)
      setPda(currentPda)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()

    const source = new EventSource('/api/mate/events', { withCredentials: true })
    source.addEventListener('operation', () => void load())
    return () => source.close()
  }, [])

  const activeAssignment = useMemo(() => {
    if (currentSession) {
      return assignments.find((a) => a.id === currentSession.assignmentId) ?? null
    }
    return assignments.find(
      (a) => a.status === 'IN_PROGRESS' || a.status === 'ASSIGNED'
    ) ?? null
  }, [assignments, currentSession])

  if (loading) {
    return <div className="mate-loading">업무 현황을 불러오는 중…</div>
  }

  return (
    <div className="mate-page mate-home-page">
      <section className="mate-greeting">
        <span>오늘도 안전하게.</span>
        <h1>현재 업무 현황</h1>
      </section>

      <div className="mate-mini-grid">
        <article>
          <Smartphone size={18} />
          <span>PDA</span>
          <strong>{pda ? `#${pda.deviceNumber}` : '미할당'}</strong>
        </article>
        <article>
          <Clock3 size={18} />
          <span>작업시간</span>
          <strong>{currentSession ? '기록 중' : '대기'}</strong>
        </article>
      </div>

      <section className="mate-section">
        <div className="mate-section-title">
          <h2>배정업무</h2>
          <Link to="/mate/work">전체 보기 <ArrowRight size={15} /></Link>
        </div>

        {activeAssignment ? (
          <article className="mate-current-work-card">
            <div className="mate-current-work-top">
              <span className={`mate-work-state ${currentSession ? 'live' : ''}`}>
                {currentSession ? 'WORKING' : activeAssignment.status}
              </span>
              <small>#{activeAssignment.id}</small>
            </div>

            <h3>{activeAssignment.workTypeName}</h3>

            <div className="mate-work-location">
              <MapPin size={17} />
              <div>
                <span>{activeAssignment.areaLocation}</span>
                <strong>
                  {activeAssignment.currentLastCompletedLocation
                    ?? `시작 ${activeAssignment.startLocation}`}
                </strong>
              </div>
            </div>

            <Link className="mate-primary-link" to="/mate/work">
              업무 열기
              <ArrowRight size={18} />
            </Link>
          </article>
        ) : (
          <div className="mate-empty-card">
            <PackageCheck size={26} />
            <strong>현재 배정된 업무가 없습니다.</strong>
            <span>관리자의 다음 배정을 기다려주세요.</span>
          </div>
        )}
      </section>

      <section className="mate-section">
        <div className="mate-section-title">
          <h2>최근 공지</h2>
          <Link to="/mate/notices">더보기 <ArrowRight size={15} /></Link>
        </div>

        <div className="mate-notice-preview">
          {notices.length === 0 && (
            <div className="mate-empty-line">표시 중인 공지가 없습니다.</div>
          )}
          {notices.slice(0, 3).map((notice) => (
            <article key={notice.id}>
              <BellRing size={16} />
              <div>
                <div>
                  {notice.important && <b>중요</b>}
                  <time>{dateTime(notice.updatedAt)}</time>
                </div>
                <p>{notice.content}</p>
              </div>
            </article>
          ))}
        </div>
      </section>
    </div>
  )
}
