import { useEffect, useState } from 'react'
import {
  CheckCircle2,
  ExternalLink,
  Info,
  RefreshCw,
  ShieldAlert
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import type { SystemReadiness } from '../api/types'
import { Panel } from '../components/Panel'

function stamp(value: string) {
  return new Intl.DateTimeFormat('ko-KR', {
    year: '2-digit',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).format(new Date(value))
}

const levelLabel: Record<string, string> = {
  BLOCKER: '시연 전 확인',
  OK: '정상',
  INFO: '운영 정보'
}

export function SystemReadinessPage() {
  const [data, setData] =
    useState<SystemReadiness | null>(null)
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState('')

  const load = async () => {
    setLoading(true)
    setMessage('')

    try {
      setData(await api.systemReadiness())
    } catch (e) {
      setMessage(
        e instanceof Error
          ? e.message
          : '시스템 점검 결과를 불러오지 못했습니다.'
      )
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()

    const source = new EventSource(
      '/api/admin/events',
      { withCredentials: true }
    )

    source.addEventListener(
      'operation',
      () => void load()
    )

    const timer = window.setInterval(
      () => void load(),
      60000
    )

    return () => {
      source.close()
      window.clearInterval(timer)
    }
  }, [])

  return (
    <div className="stack-page">
      <div className="page-title-row">
        <div>
          <span className="eyebrow">
            SYSTEM READINESS
          </span>
          <h2>시연·운영 준비 점검</h2>
          <p>
            Master Data, 정합성, Session 보안 상태를 한 화면에서 확인합니다.
            운영상 정상적으로 존재할 수 있는 미처리 업무는 시연 차단조건으로 보지 않습니다.
          </p>
        </div>

        <button
          className="secondary-button"
          disabled={loading}
          onClick={() => void load()}
        >
          <RefreshCw size={13}/>
          다시 점검
        </button>
      </div>

      {message && (
        <div className="toast-inline">{message}</div>
      )}

      {data && (
        <>
          <div className="erp-readiness-head">
            <div
              className={
                data.readyForDemo
                  ? 'ok'
                  : 'danger'
              }
            >
              <span>시연 준비상태</span>
              <strong>
                {data.readyForDemo
                  ? 'READY'
                  : 'CHECK REQUIRED'}
              </strong>
            </div>

            <div>
              <span>인증</span>
              <strong>SESSION / COOKIE</strong>
            </div>

            <div>
              <span>CSRF</span>
              <strong>
                {data.csrfEnabled
                  ? 'ENABLED'
                  : 'DISABLED'}
              </strong>
            </div>

            <div>
              <span>Demo Scenario</span>
              <strong>
                {data.demoScenarioEnabled
                  ? 'ENABLED'
                  : 'OFF'}
              </strong>
            </div>

            <div>
              <span>점검시각</span>
              <strong className="erp-readiness-time">
                {stamp(data.generatedAt)}
              </strong>
            </div>
          </div>

          <div className="erp-readiness-counts">
            <div><span>MATE</span><strong>{data.counts.activeMates}</strong></div>
            <div><span>PDA</span><strong>{data.counts.activePdas}</strong></div>
            <div><span>Location</span><strong>{data.counts.locations}</strong></div>
            <div><span>WorkType</span><strong>{data.counts.workTypes}</strong></div>
            <div><span>IssueType</span><strong>{data.counts.issueTypes}</strong></div>
            <div><span>Open Session</span><strong>{data.counts.openSessions}</strong></div>
            <div><span>Handover</span><strong>{data.counts.handoverCandidates}</strong></div>
            <div><span>Unresolved Issue</span><strong>{data.counts.unresolvedIssues}</strong></div>
            <div className={data.counts.integrityCritical ? 'danger' : ''}>
              <span>Integrity Critical</span>
              <strong>{data.counts.integrityCritical}</strong>
            </div>
            <div className={data.counts.integrityWarning ? 'warn' : ''}>
              <span>Integrity Warning</span>
              <strong>{data.counts.integrityWarning}</strong>
            </div>
          </div>
        </>
      )}

      <Panel title="준비상태 체크">
        <div className="table-wrap">
          <table className="erp-readiness-table">
            <thead>
              <tr>
                <th>구분</th>
                <th>항목</th>
                <th>점검 내용</th>
                <th>후속 화면</th>
              </tr>
            </thead>
            <tbody>
              {data?.checks.map((item) => (
                <tr
                  key={item.code}
                  className={
                    item.level === 'BLOCKER'
                      ? 'readiness-blocker-row'
                      : ''
                  }
                >
                  <td>
                    <span
                      className={`erp-readiness-level ${item.level.toLowerCase()}`}
                    >
                      {item.level === 'BLOCKER'
                        ? <ShieldAlert size={12}/>
                        : item.level === 'INFO'
                          ? <Info size={12}/>
                          : <CheckCircle2 size={12}/>}
                      {levelLabel[item.level]
                        ?? item.level}
                    </span>
                  </td>

                  <td>
                    <strong>{item.label}</strong>
                    <small>{item.code}</small>
                  </td>

                  <td>{item.detail}</td>

                  <td>
                    {item.actionPath ? (
                      <Link
                        className="erp-row-button"
                        to={item.actionPath}
                      >
                        <ExternalLink size={12}/>
                        열기
                      </Link>
                    ) : '-'}
                  </td>
                </tr>
              ))}

              {!loading
                && data
                && data.checks.length === 0
                && (
                  <tr>
                    <td
                      colSpan={4}
                      className="empty-cell"
                    >
                      점검 항목이 없습니다.
                    </td>
                  </tr>
                )}
            </tbody>
          </table>
        </div>
      </Panel>

      <div className="erp-readiness-policy">
        <strong>READY 판정</strong>
        <span>
          활성 MATE/PDA/Location/업무종류/특이사항 구분이 최소 1건 이상이고
          치명 정합성 오류가 0건일 때 READY입니다.
          Open Session, 인수인계 후보, 미해결 특이사항은 실제 운영 중 정상적으로
          존재할 수 있으므로 정보로 표시하지만 READY를 막지 않습니다.
        </span>
      </div>
    </div>
  )
}
