import { useEffect, useState } from 'react'
import {
  AlertTriangle,
  ArrowRight,
  CheckCircle2,
  ExternalLink,
  RefreshCw,
  ShieldAlert
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import type { ShiftClosePreview } from '../api/types'
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
  BLOCKER: '필수 확인',
  WARNING: '인계 확인',
  OK: '정상'
}

export function ShiftClosePage() {
  const [data, setData] =
    useState<ShiftClosePreview | null>(null)
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState('')

  const load = async () => {
    setLoading(true)
    setMessage('')

    try {
      setData(await api.shiftClosePreview())
    } catch (e) {
      setMessage(
        e instanceof Error
          ? e.message
          : '교대 마감 점검을 불러오지 못했습니다.'
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
            SHIFT CLOSE PREVIEW
          </span>
          <h2>교대 마감 점검</h2>
          <p>
            교대 전 현재 운영상태를 한 번에 대조합니다.
            이 화면은 전역 마감 버튼이 아니라 확인용 체크리스트입니다.
          </p>
        </div>

        <div className="erp-generated-at">
          <Link
            className="secondary-button compact"
            to="/action-queue"
          >
            <ArrowRight size={13}/>
            후속조치
          </Link>
          <Link
            className="secondary-button compact"
            to="/handover-overview"
          >
            <ExternalLink size={13}/>
            인계요약
          </Link>
          <button
            className="secondary-button compact"
            onClick={() => void load()}
            disabled={loading}
          >
            <RefreshCw size={13}/>
            다시 점검
          </button>
        </div>
      </div>

      {message && (
        <div className="toast-inline">{message}</div>
      )}

      {data && (
        <div className="erp-shiftclose-summary">
          <div
            className={
              data.summary.blockerCount
                ? 'danger'
                : ''
            }
          >
            <span>필수 확인</span>
            <strong>
              {data.summary.blockerCount}
            </strong>
          </div>

          <div
            className={
              data.summary.warningCount
                ? 'warn'
                : ''
            }
          >
            <span>인계 확인</span>
            <strong>
              {data.summary.warningCount}
            </strong>
          </div>

          <div>
            <span>정상 항목</span>
            <strong>{data.summary.okCount}</strong>
          </div>

          <div>
            <span>운영 점검 상태</span>
            <strong className="erp-shiftclose-state">
              {data.summary.readyForHandoverReview
                ? 'BLOCKER 없음'
                : '필수 확인 필요'}
            </strong>
          </div>

          <div>
            <span>기준시각</span>
            <strong className="erp-shiftclose-time">
              {stamp(data.generatedAt)}
            </strong>
          </div>
        </div>
      )}

      {data && data.recentShiftDates.length > 0 && (
        <Panel title="최근 근무조 보고서">
          <div className="erp-recent-shift-links">
            <strong>
              실제 WorkSession이 존재하는 최근 shiftDate
            </strong>
            <div>
              {data.recentShiftDates.map(
                (shiftDate) => (
                  <Link
                    key={shiftDate}
                    className="erp-row-button"
                    to={`/reports?mode=SHIFT&shiftDate=${shiftDate}`}
                  >
                    {shiftDate}
                  </Link>
                )
              )}
            </div>
          </div>
        </Panel>
      )}

      <Panel title="마감 체크리스트">
        <div className="table-wrap">
          <table className="erp-shiftclose-table">
            <thead>
              <tr>
                <th>구분</th>
                <th>점검 항목</th>
                <th>건수</th>
                <th>확인 내용</th>
                <th>후속 화면</th>
              </tr>
            </thead>

            <tbody>
              {data?.checks.map((item) => (
                <tr
                  key={item.code}
                  className={
                    item.level === 'BLOCKER'
                      ? 'shiftclose-blocker-row'
                      : item.level === 'WARNING'
                        ? 'shiftclose-warning-row'
                        : ''
                  }
                >
                  <td>
                    <span
                      className={`erp-shiftclose-level ${item.level.toLowerCase()}`}
                    >
                      {item.level === 'BLOCKER'
                        ? <ShieldAlert size={12}/>
                        : item.level === 'WARNING'
                          ? <AlertTriangle size={12}/>
                          : <CheckCircle2 size={12}/>}
                      {levelLabel[item.level]
                        ?? item.level}
                    </span>
                  </td>

                  <td>
                    <strong>{item.label}</strong>
                    <small>{item.code}</small>
                  </td>

                  <td>
                    <strong className="erp-shiftclose-count">
                      {item.count}
                    </strong>
                  </td>

                  <td className="erp-shiftclose-description">
                    {item.description}
                  </td>

                  <td>
                    <Link
                      className="erp-row-button"
                      to={item.actionPath}
                    >
                      <ExternalLink size={12}/>
                      {item.actionLabel}
                    </Link>
                  </td>
                </tr>
              ))}

              {!loading
                && data
                && data.checks.length === 0
                && (
                  <tr>
                    <td
                      colSpan={5}
                      className="empty-cell"
                    >
                      점검 항목이 없습니다.
                    </td>
                  </tr>
                )}

              {loading && !data && (
                <tr>
                  <td
                    colSpan={5}
                    className="empty-cell"
                  >
                    현재 운영상태를 점검하는 중입니다…
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Panel>

      <div className="erp-shiftclose-policy">
        <strong>판정 기준</strong>
        <span>
          Open WorkSession과 치명 정합성 오류만 `필수 확인`으로 분류합니다.
          PDA 사용, 미처리 업무, 특이사항은 다음 교대로 정상 인계될 수 있으므로
          시스템이 강제로 막지 않고 `인계 확인`으로 표시합니다.
          실제 MATE 근무종료와 PDA 반납 흐름은 기존 기능을 그대로 사용합니다.
        </span>
      </div>
    </div>
  )
}
