import { useEffect, useMemo, useState } from 'react'
import {
  AlertTriangle,
  Clock3,
  CheckCircle2,
  ArrowRightLeft,
  PlugZap,
  RefreshCw,
  Search,
  ShieldCheck,
  Smartphone,
  TimerReset,
  UserRoundCheck
} from 'lucide-react'
import { Link, useSearchParams } from 'react-router-dom'
import { api } from '../api/client'
import type { MateOperationRow, OperationsBoard } from '../api/types'
import { Panel } from '../components/Panel'
import { StatusBadge } from '../components/StatusBadge'

function stamp(v: string | null) {
  if (!v) return '-'
  return new Intl.DateTimeFormat('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(v))
}

function elapsed(sec: number | null) {
  if (sec == null) return '-'
  const h = Math.floor(sec / 3600)
  const m = Math.floor((sec % 3600) / 60)
  return h > 0 ? `${h}시간 ${m}분` : `${m}분`
}

const attentionLabel: Record<string, string> = {
  HEARTBEAT_STALE: '통신 지연',
  SESSION_UNCERTAIN: '시간 신뢰도 확인',
  SESSION_STATUS_MISMATCH: '세션/상태 불일치',
  WORKING_WITHOUT_SESSION: '작업중이나 세션 없음',
  OFF_DUTY_WITH_PDA: '퇴근 후 PDA 미반납',
  ACTIVE_PDA_MARKED_LOST: '사용 PDA 분실표시',
  NETWORK_RECOVERY_REQUIRED: '통신 복귀 확인 필요'
}

export function OperationsPage() {
  const [searchParams] = useSearchParams()
  const [board, setBoard] = useState<OperationsBoard | null>(null)
  const [keyword, setKeyword] = useState(
    () => searchParams.get('mateId') ?? ''
  )
  const [status, setStatus] = useState('ALL')
  const [attentionOnly, setAttentionOnly] = useState(false)
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(true)
  const [, setTick] = useState(0)

  const load = async () => {
    setLoading(true)
    try {
      setBoard(await api.operationsBoard())
    } catch (e) {
      setMessage(e instanceof Error ? e.message : '운영현황을 불러오지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()

    const source = new EventSource('/api/admin/events', { withCredentials: true })
    source.addEventListener('operation', () => void load())

    const refreshTimer = window.setInterval(() => void load(), 60000)
    const clockTimer = window.setInterval(() => setTick((x) => x + 1), 30000)

    return () => {
      source.close()
      window.clearInterval(refreshTimer)
      window.clearInterval(clockTimer)
    }
  }, [])

  useEffect(() => {
    const requestedMateId =
      searchParams.get('mateId')

    if (requestedMateId) {
      setKeyword(requestedMateId)
      setStatus('ALL')
      setAttentionOnly(false)
    }
  }, [searchParams])

  const rows = useMemo(() => {
    if (!board) return []
    const q = keyword.trim().toUpperCase()

    return board.mates.filter((row) => {
      if (status !== 'ALL' && row.status !== status) return false
      if (attentionOnly && row.attentionCodes.length === 0) return false

      if (q) {
        const haystack = [
          String(row.mateId),
          row.employeeNo,
          row.nickname,
          row.whereabouts ?? '',
          row.workType ?? '',
          row.area ?? '',
          row.lastCompletedLocation ?? '',
          row.pdaNumber == null ? '' : String(row.pdaNumber)
        ].join(' ').toUpperCase()

        if (!haystack.includes(q)) return false
      }

      return true
    })
  }, [board, keyword, status, attentionOnly])

  const run = async (fn: () => Promise<unknown>, ok: string) => {
    setMessage('')
    try {
      await fn()
      setMessage(ok)
      await load()
    } catch (e) {
      setMessage(e instanceof Error ? e.message : '처리하지 못했습니다.')
    }
  }

  const cancelAssignment = async (row: MateOperationRow) => {
    if (!row.assignmentId) return
    const reason = window.prompt(
      `업무배정 #${row.assignmentId} 취소 사유를 입력하세요. (선택)`
    )
    if (reason === null) return

    await run(
      () => api.cancelWork(row.assignmentId!, reason || undefined),
      '업무배정을 취소했습니다.'
    )
  }

  const releasePda = async (row: MateOperationRow) => {
    if (!row.pdaUsageId) return
    if (!window.confirm(`PDA ${row.pdaNumber}을 관리자 회수 처리할까요?`)) return

    await run(
      () => api.forceReleasePda(row.pdaUsageId!),
      'PDA를 관리자 회수 처리했습니다.'
    )
  }

  const toggleExtension = async (row: MateOperationRow) => {
    if (row.extensionActive) {
      await run(
        () => api.cancelAdminExtension(row.mateId),
        '오늘 연장을 해제했습니다.'
      )
    } else {
      await run(
        () => api.extendMateToday(row.mateId),
        '오늘 연장을 활성화했습니다.'
      )
    }
  }

  if (loading && !board) {
    return <div className="loading-state">운영현황을 불러오는 중입니다…</div>
  }

  if (!board) {
    return (
      <div className="error-state">
        <strong>운영현황을 불러오지 못했습니다.</strong>
        <span>{message}</span>
        <button className="secondary-button" onClick={() => void load()}>다시 시도</button>
      </div>
    )
  }

  const summary = board.summary

  return (
    <div className="stack-page">
      <div className="page-title-row erp-ops-title">
        <div>
          <span className="eyebrow">LIVE OPERATIONS</span>
          <h2>운영관제</h2>
          <p>현재 MATE·PDA·작업세션·근무종료 상태를 한 화면에서 확인합니다.</p>
        </div>
        <div className="erp-generated-at">
          기준 {stamp(board.generatedAt)}
          <Link className="secondary-button compact" to="/action-queue">
            <AlertTriangle size={13}/> 후속조치
          </Link>
          <Link className="secondary-button compact" to="/handover">
            <ArrowRightLeft size={13}/> 인수인계
          </Link>
          <Link className="secondary-button compact" to="/handover-overview">
            <Clock3 size={13}/> 인계요약
          </Link>
          <Link className="secondary-button compact" to="/shift-close">
            <CheckCircle2 size={13}/> 마감점검
          </Link>
          <Link className="secondary-button compact" to="/integrity">
            <ShieldCheck size={13}/> 정합성 검사
          </Link>
          <button className="secondary-button compact" onClick={() => void load()}>
            <RefreshCw size={13}/> 갱신
          </button>
        </div>
      </div>

      {message && <div className="toast-inline">{message}</div>}

      <div className="erp-ops-summary">
        <div><span>활성 MATE</span><strong>{summary.activeMateCount}</strong></div>
        <div><span>대기</span><strong>{summary.availableMateCount}</strong></div>
        <div><span>작업중</span><strong>{summary.workingMateCount}</strong></div>
        <div><span>휴게/자리비움</span><strong>{summary.breakMateCount + summary.awayMateCount}</strong></div>
        <div><span>Open Session</span><strong>{summary.activeSessionCount}</strong></div>
        <div className={summary.uncertainSessionCount ? 'warn' : ''}>
          <span>UNCERTAIN</span><strong>{summary.uncertainSessionCount}</strong>
        </div>
        <div><span>PDA 사용중</span><strong>{summary.pdaInUseCount}</strong></div>
        <div className={summary.pdaAttentionCount ? 'warn' : ''}>
          <span>PDA 점검/분실</span><strong>{summary.pdaAttentionCount}</strong>
        </div>
        <div className={summary.unconfirmedIssueCount ? 'warn' : ''}>
          <span>미확인 특이사항</span><strong>{summary.unconfirmedIssueCount}</strong>
        </div>
        <Link
          className={`erp-ops-summary-link ${summary.unassignedOpenIssueCount ? 'warn' : ''}`}
          to="/issues?responsible=UNASSIGNED"
        >
          <span>미담당 특이사항</span>
          <strong>{summary.unassignedOpenIssueCount}</strong>
        </Link>
        <div className={summary.attentionMateCount ? 'danger' : ''}>
          <span>운영 확인필요</span><strong>{summary.attentionMateCount}</strong>
        </div>
      </div>

      <Panel title="MATE 실시간 운영현황">
        <div className="erp-filter-toolbar erp-ops-filter">
          <label>
            상태
            <select value={status} onChange={(e) => setStatus(e.target.value)}>
              <option value="ALL">전체</option>
              <option value="AVAILABLE">대기</option>
              <option value="WORKING">업무중</option>
              <option value="BREAK">휴게</option>
              <option value="AWAY">자리비움</option>
              <option value="OFF_DUTY">퇴근</option>
            </select>
          </label>

          <label className="erp-keyword-filter">
            검색
            <span>
              <Search size={13}/>
              <input
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                placeholder="MATE / 업무 / 구역 / PDA"
              />
            </span>
          </label>

          <label className="erp-ops-check">
            <input
              type="checkbox"
              checked={attentionOnly}
              onChange={(e) => setAttentionOnly(e.target.checked)}
            />
            확인필요만
          </label>

          <div className="erp-filter-count">
            조회 {rows.length} / 전체 {board.mates.length}
          </div>
        </div>

        <div className="table-wrap erp-ops-table-wrap">
          <table className="erp-ops-table">
            <thead>
              <tr>
                <th>MATE</th>
                <th>상태 / 거소</th>
                <th>PDA</th>
                <th>현재 업무</th>
                <th>진행 위치</th>
                <th>작업 세션</th>
                <th>Heartbeat</th>
                <th>근무종료</th>
                <th>확인사항</th>
                <th>운영</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.mateId} className={row.attentionCodes.length ? 'attention-row' : ''}>
                  <td>
                    <strong>{row.nickname}</strong>
                    <small>{row.employeeNo}</small>
                  </td>

                  <td>
                    <StatusBadge status={row.status}/>
                    <small>{row.whereabouts ?? '-'}</small>
                  </td>

                  <td>
                    {row.pdaNumber == null ? (
                      '-'
                    ) : (
                      <>
                        <strong>PDA {row.pdaNumber}</strong>
                        <small>{row.pdaStatus}</small>
                      </>
                    )}
                  </td>

                  <td>
                    {row.assignmentId ? (
                      <>
                        <Link to={`/assignments?assignmentId=${row.assignmentId}`}>#{row.assignmentId} {row.workType}</Link>
                        <small>{row.area} · {row.assignmentStatus}</small>
                      </>
                    ) : '-'}
                  </td>

                  <td>
                    {row.assignmentId ? (
                      <>
                        <strong>{row.lastCompletedLocation ?? '미기록'}</strong>
                        <small>시작 {row.startLocation}</small>
                      </>
                    ) : '-'}
                  </td>

                  <td>
                    {row.openSessionId ? (
                      <>
                        <strong>{elapsed(row.elapsedSeconds)}</strong>
                        <small>#{row.openSessionId} · {row.sessionQuality}</small>
                      </>
                    ) : '-'}
                  </td>

                  <td>
                    {row.openSessionId ? (
                      <>
                        <strong>{stamp(row.lastHeartbeatAt)}</strong>
                        <small>{row.lastHeartbeatAt ? '최근 통신' : '-'}</small>
                      </>
                    ) : '-'}
                  </td>

                  <td>
                    {row.extensionActive ? (
                      <>
                        <strong className="erp-extension-on">연장</strong>
                        <small>{row.shiftDate} 기준 · 수동 종료</small>
                      </>
                    ) : (
                      <>
                        <strong>{row.effectiveScheduledEnd ? stamp(row.effectiveScheduledEnd) : '-'}</strong>
                        <small>{row.shiftDate} 기준 · 자동 종료 예정</small>
                      </>
                    )}
                  </td>

                  <td>
                    {row.attentionCodes.length === 0 ? (
                      <span className="erp-ok-text">정상</span>
                    ) : (
                      <div className="erp-attention-list">
                        {row.attentionCodes.map((code) => (
                          <span key={code}>
                            <AlertTriangle size={11}/>
                            {attentionLabel[code] ?? code}
                          </span>
                        ))}
                      </div>
                    )}
                  </td>

                  <td>
                    <div className="erp-ops-actions">
                      <Link
                        className="erp-ops-link-button"
                        to={`/mates?mateId=${row.mateId}`}
                        title="MATE 상세"
                      >
                        <UserRoundCheck size={12}/>
                        MATE
                      </Link>

                      {row.assignmentId && !row.openSessionId && (
                        <Link
                          className="erp-ops-link-button"
                          to={`/handover?assignmentId=${row.assignmentId}`}
                          title="현재 세션이 없는 업무의 인수인계/재개 상태 확인"
                        >
                          <ArrowRightLeft size={12}/>
                          인수인계
                        </Link>
                      )}

                      {row.attentionCodes.some((code) =>
                        [
                          'SESSION_STATUS_MISMATCH',
                          'WORKING_WITHOUT_SESSION',
                          'OFF_DUTY_WITH_PDA',
                          'ACTIVE_PDA_MARKED_LOST'
                        ].includes(code)
                      ) && (
                        <Link
                          className="erp-ops-link-button"
                          to="/integrity"
                          title="관련 데이터 정합성 검사"
                        >
                          <ShieldCheck size={12}/>
                          정합성
                        </Link>
                      )}

                      {!row.assignmentId && row.status !== 'OFF_DUTY' && (
                        <Link
                          className="erp-ops-link-button"
                          to={`/assignments?mateId=${row.mateId}`}
                        >
                          <UserRoundCheck size={12}/> 업무배정
                        </Link>
                      )}

                      <button
                        title="당일 연장 상태 변경"
                        onClick={() => void toggleExtension(row)}
                      >
                        <TimerReset size={12}/>
                        {row.extensionActive ? '연장해제' : '연장'}
                      </button>

                      {row.assignmentId && row.assignmentStatus !== 'COMPLETED' && row.assignmentStatus !== 'CANCELED' && (
                        <button
                          disabled={Boolean(row.openSessionId)}
                          title={row.openSessionId ? '작업 중 세션을 먼저 일시정지해야 합니다.' : '업무배정 취소'}
                          onClick={() => void cancelAssignment(row)}
                        >
                          <PlugZap size={12}/> 배정취소
                        </button>
                      )}

                      {row.pdaUsageId && (
                        <button
                          disabled={Boolean(row.openSessionId)}
                          title={row.openSessionId ? '작업 중 세션을 먼저 일시정지해야 합니다.' : 'PDA 관리자 회수'}
                          onClick={() => void releasePda(row)}
                        >
                          <Smartphone size={12}/> PDA회수
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}

              {rows.length === 0 && (
                <tr>
                  <td colSpan={10} className="empty-cell">조건에 맞는 운영현황이 없습니다.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Panel>

      <div className="erp-ops-footnote">
        <UserRoundCheck size={14}/>
        <span>
          확인필요 표시는 통신 지연·세션/상태 불일치·퇴근 후 PDA 미반납 같은 객관적 운영 이상만 표시합니다.
          업무 우선순위는 자동 계산하지 않습니다.
        </span>
      </div>
    </div>
  )
}
