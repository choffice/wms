import { useEffect, useMemo, useState } from 'react'
import {
  ClipboardCopy,
  ExternalLink,
  RefreshCw,
  Save
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import type { HandoverOverview } from '../api/types'
import { Panel } from '../components/Panel'

function stamp(value: string) {
  return new Intl.DateTimeFormat('ko-KR', {
    year: '2-digit',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

const issueStatusLabel: Record<string, string> = {
  UNCONFIRMED: '미확인',
  CONFIRMED: '확인',
  RESOLVED: '해결'
}

export function HandoverOverviewPage() {
  const [data, setData] =
    useState<HandoverOverview | null>(null)
  const [note, setNote] = useState('')
  const [noteShiftDate, setNoteShiftDate] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState('')

  const load = async () => {
    setLoading(true)

    try {
      setData(await api.handoverOverview())
    } catch (e) {
      setMessage(
        e instanceof Error
          ? e.message
          : '인계요약을 불러오지 못했습니다.'
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

  const copyText = useMemo(() => {
    if (!data) return ''

    const lines = [
      '[WAREHOUSE 교대 인계요약]',
      `기준 ${stamp(data.generatedAt)}`,
      '',
      ...data.summaryLines.map(
        (line) => `- ${line}`
      ),
      '',
      '[미처리 업무]',
      ...data.assignments.slice(0, 10).map(
        (item) =>
          `- #${item.assignmentId} ${item.workType} / ${item.area} / ${item.currentMate} / ${item.lastLocation} / ${item.stateLabel}`
      ),
      '',
      '[미해결 특이사항]',
      ...data.issues.slice(0, 10).map(
        (item) =>
          `- #${item.issueId} ${item.issueType} / ${issueStatusLabel[item.status] ?? item.status} / ${item.responsible ?? '미담당'} / ${item.location ?? '-'}`
      ),
      '',
      '[최근 인계메모]',
      ...data.recentNotes.slice(0, 5).map(
        (item) =>
          `- ${item.actor} ${stamp(item.createdAt)}${item.shiftDate ? ` [shift ${item.shiftDate}]` : ''}: ${item.content}`
      )
    ]

    return lines.join('\n')
  }, [data])

  const copySummary = async () => {
    if (!copyText) return

    try {
      await navigator.clipboard.writeText(
        copyText
      )
      setMessage(
        '현재 인계요약을 클립보드에 복사했습니다.'
      )
    } catch {
      setMessage(
        '브라우저에서 클립보드 복사를 허용하지 않았습니다.'
      )
    }
  }

  const saveNote = async () => {
    const content = note.trim()
    if (!content) return

    setSaving(true)
    setMessage('')

    try {
      await api.createHandoverNote(
        content,
        noteShiftDate || undefined
      )
      setNote('')
      setNoteShiftDate('')
      setMessage('교대 인계메모를 추가했습니다.')
      await load()
    } catch (e) {
      setMessage(
        e instanceof Error
          ? e.message
          : '인계메모를 저장하지 못했습니다.'
      )
    } finally {
      setSaving(false)
    }
  }

  if (loading && !data) {
    return (
      <div className="loading-state">
        현재 인계요약을 생성하는 중입니다…
      </div>
    )
  }

  return (
    <div className="stack-page">
      <div className="page-title-row">
        <div>
          <span className="eyebrow">
            HANDOVER OVERVIEW
          </span>
          <h2>교대 인계요약</h2>
          <p>
            미처리 업무·특이사항·정합성·최근 관리자 처리내역과
            인계메모를 한 화면에 모읍니다.
          </p>
        </div>

        <div className="erp-generated-at">
          {data && `기준 ${stamp(data.generatedAt)}`}

          <button
            className="secondary-button compact"
            onClick={() => void copySummary()}
            disabled={!data}
          >
            <ClipboardCopy size={13}/>
            요약 복사
          </button>

          <button
            className="secondary-button compact"
            onClick={() => void load()}
            disabled={loading}
          >
            <RefreshCw size={13}/>
            갱신
          </button>
        </div>
      </div>

      {message && (
        <div className="toast-inline">{message}</div>
      )}

      {data && (
        <div className="erp-handover-overview-summary">
          <div>
            <span>미처리 업무</span>
            <strong>
              {data.counts.pendingAssignments}
            </strong>
          </div>
          <div
            className={
              data.counts.handoverCandidates
                ? 'warn'
                : ''
            }
          >
            <span>인수인계 검토</span>
            <strong>
              {data.counts.handoverCandidates}
            </strong>
          </div>
          <div>
            <span>미해결 특이사항</span>
            <strong>
              {data.counts.unresolvedIssues}
            </strong>
          </div>
          <div
            className={
              data.counts.integrityCritical
                ? 'danger'
                : ''
            }
          >
            <span>정합성 치명</span>
            <strong>
              {data.counts.integrityCritical}
            </strong>
          </div>
          <div>
            <span>Open Session</span>
            <strong>
              {data.counts.openSessions}
            </strong>
          </div>
          <div
            className={
              data.counts.operationAttentionMates
                ? 'warn'
                : ''
            }
          >
            <span>운영 Attention</span>
            <strong>
              {data.counts.operationAttentionMates}
            </strong>
          </div>
        </div>
      )}

      <Panel title="자동 생성 인계요약">
        <div className="erp-handover-summary-lines">
          {data?.summaryLines.map((line) => (
            <div key={line}>
              <span>•</span>
              <strong>{line}</strong>
            </div>
          ))}
        </div>

        {data && data.recentShiftDates.length > 0 && (
          <div className="erp-recent-shift-links">
            <strong>최근 근무조 보고서</strong>
            <div>
              {data.recentShiftDates.map((shiftDate) => (
                <Link
                  key={shiftDate}
                  className="erp-row-button"
                  to={`/reports?mode=SHIFT&shiftDate=${shiftDate}`}
                >
                  {shiftDate}
                </Link>
              ))}
            </div>
          </div>
        )}

        <div className="erp-handover-overview-links">
          <Link
            className="erp-row-button"
            to="/action-queue"
          >
            후속조치 큐
          </Link>
          <Link
            className="erp-row-button"
            to="/handover"
          >
            인수인계 업무
          </Link>
          <Link
            className="erp-row-button"
            to="/issues"
          >
            특이사항
          </Link>
          <Link
            className="erp-row-button"
            to="/integrity"
          >
            정합성
          </Link>
          <Link
            className="erp-row-button"
            to="/reports"
          >
            근무조 보고서
          </Link>
        </div>
      </Panel>

      <div className="erp-handover-overview-grid">
        <Panel title="미처리 업무">
          <div className="table-wrap erp-handover-overview-table">
            <table>
              <thead>
                <tr>
                  <th>Assignment</th>
                  <th>상태</th>
                  <th>업무 / 구역</th>
                  <th>MATE</th>
                  <th>이어갈 위치</th>
                  <th>마지막 종료</th>
                  <th>열기</th>
                </tr>
              </thead>
              <tbody>
                {data?.assignments.map((item) => (
                  <tr key={item.assignmentId}>
                    <td>
                      <strong>
                        #{item.assignmentId}
                      </strong>
                    </td>
                    <td>{item.stateLabel}</td>
                    <td>
                      <strong>{item.workType}</strong>
                      <small>{item.area}</small>
                    </td>
                    <td>{item.currentMate}</td>
                    <td>
                      <strong>
                        {item.lastLocation}
                      </strong>
                    </td>
                    <td>
                      {item.lastSessionEndReason ?? '-'}
                    </td>
                    <td>
                      <Link
                        className="erp-row-button"
                        to={`/handover?assignmentId=${item.assignmentId}`}
                      >
                        <ExternalLink size={12}/>
                        열기
                      </Link>
                    </td>
                  </tr>
                ))}

                {data?.assignments.length === 0 && (
                  <tr>
                    <td
                      colSpan={7}
                      className="empty-cell"
                    >
                      미처리 활성업무가 없습니다.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </Panel>

        <Panel title="미해결 특이사항">
          <div className="table-wrap erp-handover-overview-table">
            <table>
              <thead>
                <tr>
                  <th>No.</th>
                  <th>상태</th>
                  <th>구분</th>
                  <th>담당</th>
                  <th>위치</th>
                  <th>내용</th>
                  <th>열기</th>
                </tr>
              </thead>
              <tbody>
                {data?.issues.map((item) => (
                  <tr key={item.issueId}>
                    <td>
                      <strong>#{item.issueId}</strong>
                    </td>
                    <td>
                      {issueStatusLabel[item.status]
                        ?? item.status}
                    </td>
                    <td>{item.issueType}</td>
                    <td>
                      {item.responsible ?? (
                        <span className="erp-unassigned-text">
                          미담당
                        </span>
                      )}
                    </td>
                    <td>{item.location ?? '-'}</td>
                    <td className="erp-overview-comment">
                      {item.comment}
                    </td>
                    <td>
                      <Link
                        className="erp-row-button"
                        to={`/issues?issueId=${item.issueId}`}
                      >
                        <ExternalLink size={12}/>
                        열기
                      </Link>
                    </td>
                  </tr>
                ))}

                {data?.issues.length === 0 && (
                  <tr>
                    <td
                      colSpan={7}
                      className="empty-cell"
                    >
                      미해결 특이사항이 없습니다.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </Panel>
      </div>

      <div className="erp-handover-overview-grid">
        <Panel title="교대 인계메모">
          <div className="erp-handover-note-editor">
            <div className="erp-note-shift-row">
              <label>
                연결 근무조 <small>선택</small>
                <select
                  value={noteShiftDate}
                  onChange={(e) =>
                    setNoteShiftDate(e.target.value)
                  }
                >
                  <option value="">
                    공통 인계메모
                  </option>
                  {data?.recentShiftDates.map(
                    (shiftDate) => (
                      <option
                        key={shiftDate}
                        value={shiftDate}
                      >
                        {shiftDate}
                      </option>
                    )
                  )}
                </select>
              </label>
            </div>

            <label>
              새 메모
              <textarea
                value={note}
                maxLength={1200}
                onChange={(e) =>
                  setNote(e.target.value)
                }
                placeholder="다음 관리자에게 남길 현장 특이점, 확인 필요사항 등을 입력합니다."
              />
            </label>

            <div className="erp-note-save-row">
              <small>
                저장된 메모는 수정/삭제하지 않고
                작성자와 시각을 그대로 남깁니다.
              </small>

              <button
                className="primary-button"
                disabled={
                  saving || !note.trim()
                }
                onClick={() => void saveNote()}
              >
                <Save size={13}/>
                메모 추가
              </button>
            </div>
          </div>

          <div className="erp-handover-note-list">
            {data?.recentNotes.map((item) => (
              <div key={item.id}>
                <div>
                  <strong>{item.actor}</strong>
                  <small>
                    {stamp(item.createdAt)}
                    {item.shiftDate
                      ? ` · shift ${item.shiftDate}`
                      : ' · 공통'}
                    {' · #'}
                    {item.id}
                  </small>
                </div>
                <p>{item.content}</p>
              </div>
            ))}

            {data?.recentNotes.length === 0 && (
              <div className="empty-cell">
                아직 인계메모가 없습니다.
              </div>
            )}
          </div>
        </Panel>

        <Panel title="최근 관리자 처리내역">
          <div className="table-wrap erp-recent-admin-actions">
            <table>
              <thead>
                <tr>
                  <th>시각</th>
                  <th>관리자</th>
                  <th>구분</th>
                  <th>대상</th>
                  <th>내용</th>
                </tr>
              </thead>
              <tbody>
                {data?.recentAdminActions.map(
                  (item) => (
                    <tr key={item.id}>
                      <td>{stamp(item.createdAt)}</td>
                      <td>{item.actor ?? '-'}</td>
                      <td>{item.type}</td>
                      <td>{item.target ?? '-'}</td>
                      <td>{item.message}</td>
                    </tr>
                  )
                )}

                {data?.recentAdminActions.length === 0 && (
                  <tr>
                    <td
                      colSpan={5}
                      className="empty-cell"
                    >
                      최근 관리자 처리이력이 없습니다.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </Panel>
      </div>

      <div className="erp-handover-overview-policy">
        <strong>인계 기준</strong>
        <span>
          이 화면은 특정 근무조의 시작/종료 시각을 추정하지 않습니다.
          현재 Snapshot과 최근 관리자 처리내역을 정리하며,
          실제 근무종료·PDA 반납·업무 재개는 각 기존 운영흐름을 그대로 사용합니다.
        </span>
      </div>
    </div>
  )
}
