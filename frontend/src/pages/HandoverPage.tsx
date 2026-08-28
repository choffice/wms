import { useEffect, useMemo, useState } from 'react'
import {
  AlertTriangle,
  ArrowRightLeft,
  CheckCircle2,
  ExternalLink,
  RefreshCw,
  Search,
  UserRoundCheck
} from 'lucide-react'
import { Link, useSearchParams } from 'react-router-dom'
import { api } from '../api/client'
import type { HandoverBoard, HandoverRow, Mate } from '../api/types'
import { Panel } from '../components/Panel'
import { StatusBadge } from '../components/StatusBadge'

function stamp(value: string | null) {
  if (!value) return '-'

  return new Intl.DateTimeFormat('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

const stateLabel: Record<string, string> = {
  NETWORK_RECOVERY: '통신 복귀 확인',
  OFF_DUTY_HANDOVER: '퇴근 인수인계',
  SHIFT_CARRYOVER: '근무종료 이월',
  ASSIGNED_NOT_STARTED: '미시작 배정',
  PAUSED: '일시정지',
  READY_TO_RESUME: '재개 대기'
}

export function HandoverPage() {
  const [searchParams] = useSearchParams()
  const [board, setBoard] = useState<HandoverBoard | null>(null)
  const [mates, setMates] = useState<Mate[]>([])
  const [state, setState] = useState(() => {
    const requested = searchParams.get('state')
    return requested && stateLabel[requested]
      ? requested
      : 'ALL'
  })
  const [handoverOnly, setHandoverOnly] = useState(false)
  const [keyword, setKeyword] = useState(
    () => searchParams.get('assignmentId') ?? ''
  )
  const [selectedMate, setSelectedMate] = useState<Record<number, string>>({})
  const [reason, setReason] = useState<Record<number, string>>({})
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(true)
  const [pendingId, setPendingId] = useState<number | null>(null)
  const [selectedIds, setSelectedIds] = useState<number[]>([])
  const [bulkMateId, setBulkMateId] = useState('')
  const [bulkReason, setBulkReason] = useState('')
  const [bulkPending, setBulkPending] = useState(false)

  const load = async () => {
    setLoading(true)

    try {
      const [handover, mateRows] = await Promise.all([
        api.handoverBoard(),
        api.mates()
      ])

      setBoard(handover)
      setMates(
        mateRows
          .filter((mate) => mate.active)
          .sort((a, b) => {
            const rank = (status: string) =>
              status === 'AVAILABLE'
                ? 0
                : status === 'BREAK'
                  ? 1
                  : status === 'AWAY'
                    ? 2
                    : status === 'WORKING'
                      ? 3
                      : 4

            return rank(a.status) - rank(b.status)
              || a.nickname.localeCompare(b.nickname, 'ko')
          })
      )
    } catch (e) {
      setMessage(
        e instanceof Error
          ? e.message
          : '미처리 업무를 불러오지 못했습니다.'
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

  useEffect(() => {
    const requestedAssignmentId =
      searchParams.get('assignmentId') ?? ''
    const requestedState =
      searchParams.get('state') ?? ''

    if (requestedAssignmentId) {
      setKeyword(requestedAssignmentId)
    }

    if (
      requestedState
        && stateLabel[requestedState]
    ) {
      setState(requestedState)
    }
  }, [searchParams])

  const rows = useMemo(() => {
    if (!board) return []

    const q = keyword.trim().toUpperCase()

    return board.rows.filter((row) => {
      if (
        state !== 'ALL'
          && row.handoverState !== state
      ) return false

      if (
        handoverOnly
          && !row.handoverCandidate
      ) return false

      if (!q) return true

      return [
        String(row.assignmentId),
        row.currentMateNickname,
        row.employeeNo,
        row.workType,
        row.area,
        row.startLocation,
        row.currentLastCompletedLocation ?? '',
        row.currentMateStatus,
        row.currentMateWhereabouts ?? '',
        row.lastSessionEndReason ?? '',
        row.currentPdaNumber == null
          ? ''
          : String(row.currentPdaNumber)
      ].join(' ').toUpperCase().includes(q)
    })
  }, [board, state, handoverOnly, keyword])

  const visibleIds = useMemo(
    () => rows.map((row) => row.assignmentId),
    [rows]
  )

  const selectedRows = useMemo(
    () => rows.filter((row) =>
      selectedIds.includes(row.assignmentId)
    ),
    [rows, selectedIds]
  )

  const allVisibleSelected =
    rows.length > 0
      && visibleIds.every((id) =>
        selectedIds.includes(id)
      )

  const toggleRow = (assignmentId: number) => {
    setSelectedIds((prev) => {
      if (prev.includes(assignmentId)) {
        return prev.filter(
          (id) => id !== assignmentId
        )
      }

      if (prev.length >= 50) {
        setMessage(
          '일괄 인수인계는 한 번에 최대 50건까지 선택할 수 있습니다.'
        )
        return prev
      }

      return [...prev, assignmentId]
    })
  }

  const toggleVisible = () => {
    setSelectedIds((prev) => {
      if (allVisibleSelected) {
        return prev.filter(
          (id) => !visibleIds.includes(id)
        )
      }

      const merged = Array.from(
        new Set([...prev, ...visibleIds])
      )

      if (merged.length > 50) {
        setMessage(
          '일괄 인수인계는 한 번에 최대 50건까지 선택할 수 있습니다.'
        )
      }

      return merged.slice(0, 50)
    })
  }

  const selectVisibleCandidates = () => {
    const candidateIds = rows
      .filter((row) => row.handoverCandidate)
      .map((row) => row.assignmentId)

    setSelectedIds((prev) => {
      const merged = Array.from(
        new Set([...prev, ...candidateIds])
      )

      if (merged.length > 50) {
        setMessage(
          '인수인계 검토건 중 앞 50건까지만 선택했습니다.'
        )
      }

      return merged.slice(0, 50)
    })
  }

  const applyBulkMate = () => {
    if (!bulkMateId || selectedRows.length === 0) return

    setSelectedMate((prev) => {
      const next = { ...prev }

      for (const row of selectedRows) {
        if (Number(bulkMateId) === row.currentMateId) {
          continue
        }
        next[row.assignmentId] = bulkMateId
      }

      return next
    })
  }

  const bulkTransfer = async () => {
    if (selectedRows.length === 0) return

    const missingTarget =
      selectedRows.find(
        (row) => !selectedMate[row.assignmentId]
      )

    if (missingTarget) {
      setMessage(
        `Assignment #${missingTarget.assignmentId}의 새 담당 MATE를 선택해주세요.`
      )
      return
    }

    const workingTargets = selectedRows
      .map((row) =>
        mates.find(
          (mate) =>
            mate.id
              === Number(
                selectedMate[row.assignmentId]
              )
        )
      )
      .filter(
        (mate): mate is Mate =>
          Boolean(
            mate && mate.status === 'WORKING'
          )
      )

    const workingNote =
      workingTargets.length > 0
        ? `\n\n현재 WORKING인 새 담당자가 ${workingTargets.length}명 포함되어 있습니다. 담당 변경은 되지만 실제 재개는 기존 세션 종료 후 가능합니다.`
        : ''

    if (
      !window.confirm(
        `선택 ${selectedRows.length}건을 일괄 인수인계할까요?`
          + `\n\n한 건이라도 담당자 변경·Open Session 발생 등 검증에 실패하면 전체를 롤백합니다.`
          + workingNote
      )
    ) return

    setBulkPending(true)
    setMessage('')

    try {
      const result = await api.bulkHandover(
        selectedRows.map((row) => ({
          assignmentId: row.assignmentId,
          expectedCurrentMateId:
            row.currentMateId,
          toMateId:
            Number(
              selectedMate[row.assignmentId]
            ),
          reason:
            reason[row.assignmentId]?.trim()
              || bulkReason.trim()
              || `교대 인수인계 · ${
                stateLabel[row.handoverState]
                  ?? row.handoverState
              }`
        }))
      )

      setMessage(
        `${result.processedCount}건의 인수인계를 일괄 처리했습니다.`
      )
      setSelectedIds([])
      setBulkMateId('')
      setBulkReason('')
      setSelectedMate({})
      setReason({})
      await load()
    } catch (e) {
      setMessage(
        e instanceof Error
          ? e.message
          : '일괄 인수인계를 처리하지 못했습니다.'
      )
    } finally {
      setBulkPending(false)
    }
  }

  useEffect(() => {
    setSelectedIds((prev) =>
      prev.filter((id) => visibleIds.includes(id))
    )
  }, [state, handoverOnly, keyword, board])

  const candidates = (
    row: HandoverRow
  ) => mates.filter(
    (mate) => mate.id !== row.currentMateId
  )

  const reassign = async (
    row: HandoverRow
  ) => {
    const toMateId =
      Number(selectedMate[row.assignmentId])

    if (!toMateId) return

    const target =
      mates.find(
        (mate) => mate.id === toMateId
      )

    if (!target) return

    if (
      target.status === 'WORKING'
        && !window.confirm(
          `${target.nickname} MATE는 현재 WORKING 상태입니다.\n\n담당 변경 자체는 가능하지만 새 업무 시작은 현재 작업세션 종료 후 가능합니다. 계속할까요?`
        )
    ) return

    if (
      !window.confirm(
        `Assignment #${row.assignmentId}\n${row.currentMateNickname} → ${target.nickname}\n\n현재 마지막 수행위치 ${row.currentLastCompletedLocation ?? '미기록'}를 그대로 유지하고 담당자만 변경합니다.`
      )
    ) return

    setPendingId(row.assignmentId)
    setMessage('')

    try {
      await api.bulkHandover([
        {
          assignmentId: row.assignmentId,
          expectedCurrentMateId:
            row.currentMateId,
          toMateId,
          reason:
            reason[row.assignmentId]?.trim()
              || `인수인계 · ${
                stateLabel[row.handoverState]
                  ?? row.handoverState
              }`
        }
      ])

      setMessage(
        `Assignment #${row.assignmentId} 담당자를 ${target.nickname}(으)로 변경했습니다.`
      )

      setSelectedMate((prev) => ({
        ...prev,
        [row.assignmentId]: ''
      }))

      setReason((prev) => ({
        ...prev,
        [row.assignmentId]: ''
      }))

      await load()
    } catch (e) {
      setMessage(
        e instanceof Error
          ? e.message
          : '담당자를 변경하지 못했습니다.'
      )
    } finally {
      setPendingId(null)
    }
  }

  if (loading && !board) {
    return (
      <div className="loading-state">
        미처리 업무와 인수인계 상태를 확인하는 중입니다…
      </div>
    )
  }

  if (!board) {
    return (
      <div className="error-state">
        <strong>인수인계 현황을 불러오지 못했습니다.</strong>
        <span>{message}</span>
        <button
          className="secondary-button"
          onClick={() => void load()}
        >
          다시 시도
        </button>
      </div>
    )
  }

  return (
    <div className="stack-page">
      <div className="page-title-row">
        <div>
          <span className="eyebrow">HANDOVER / CARRYOVER</span>
          <h2>미처리 업무 / 인수인계</h2>
          <p>
            Open WorkSession이 없는 활성 업무를 모아 재개·이월·담당변경이 필요한 건을 확인합니다.
          </p>
        </div>

        <div className="erp-generated-at">
          기준 {stamp(board.generatedAt)}
          <Link
            className="secondary-button compact"
            to="/handover-overview"
          >
            <ExternalLink size={13}/>
            인계요약
          </Link>
          <Link
            className="secondary-button compact"
            to="/shift-close"
          >
            <CheckCircle2 size={13}/>
            마감점검
          </Link>
          <button
            className="secondary-button compact"
            onClick={() => void load()}
          >
            <RefreshCw size={13}/>
            갱신
          </button>
        </div>
      </div>

      {message && (
        <div className="toast-inline">{message}</div>
      )}

      <div className="erp-handover-summary">
        <div>
          <span>미처리 활성업무</span>
          <strong>{board.summary.pendingCount}</strong>
        </div>
        <div
          className={
            board.summary.handoverCandidateCount
              ? 'warn'
              : ''
          }
        >
          <span>인수인계 검토</span>
          <strong>
            {board.summary.handoverCandidateCount}
          </strong>
        </div>
        <div>
          <span>미시작</span>
          <strong>
            {board.summary.assignedNotStartedCount}
          </strong>
        </div>
        <div>
          <span>일시정지/재개</span>
          <strong>{board.summary.pausedCount}</strong>
        </div>
        <div
          className={
            board.summary.networkRecoveryCount
              ? 'danger'
              : ''
          }
        >
          <span>통신 복귀확인</span>
          <strong>
            {board.summary.networkRecoveryCount}
          </strong>
        </div>
        <div>
          <span>퇴근/근무종료 이월</span>
          <strong>{board.summary.offDutyCount}</strong>
        </div>
        <div
          className={
            board.summary.mateBusyElsewhereCount
              ? 'warn'
              : ''
          }
        >
          <span>담당 MATE 다른 업무중</span>
          <strong>
            {board.summary.mateBusyElsewhereCount}
          </strong>
        </div>
      </div>

      <Panel title="조회 조건">
        <div className="erp-handover-filter">
          <label>
            운영상태
            <select
              value={state}
              onChange={(e) => setState(e.target.value)}
            >
              <option value="ALL">전체</option>
              {Object.entries(stateLabel).map(
                ([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                )
              )}
            </select>
          </label>

          <label className="erp-handover-check">
            <input
              type="checkbox"
              checked={handoverOnly}
              onChange={(e) =>
                setHandoverOnly(e.target.checked)
              }
            />
            인수인계 검토만
          </label>

          <label className="erp-keyword-filter">
            검색
            <span>
              <Search size={13}/>
              <input
                value={keyword}
                onChange={(e) =>
                  setKeyword(e.target.value)
                }
                placeholder="Assignment / MATE / 업무 / 위치 / PDA"
              />
            </span>
          </label>

          <div className="erp-filter-count">
            조회 {rows.length}
            {' / '}
            전체 {board.rows.length}
          </div>
        </div>
      </Panel>

      <Panel
        title={`선택 인수인계 · ${selectedRows.length}건`}
      >
        <div className="erp-handover-bulk">
          <div className="erp-handover-bulk-actions">
            <button
              className="secondary-button compact"
              onClick={selectVisibleCandidates}
              disabled={
                bulkPending || rows.length === 0
              }
            >
              인수인계 검토건 선택
            </button>
            <button
              className="secondary-button compact"
              onClick={() => setSelectedIds([])}
              disabled={
                bulkPending
                  || selectedIds.length === 0
              }
            >
              선택 해제
            </button>
          </div>

          <label>
            선택 건 공통 담당
            <select
              value={bulkMateId}
              onChange={(e) =>
                setBulkMateId(e.target.value)
              }
            >
              <option value="">선택</option>
              {mates.map((mate) => (
                <option
                  key={mate.id}
                  value={mate.id}
                >
                  {mate.nickname}
                  {' · '}
                  {mate.status}
                </option>
              ))}
            </select>
          </label>

          <button
            className="secondary-button"
            onClick={applyBulkMate}
            disabled={
              !bulkMateId
                || selectedRows.length === 0
            }
          >
            선택 행에 담당 적용
          </button>

          <label className="erp-handover-bulk-reason">
            공통 사유
            <input
              value={bulkReason}
              onChange={(e) =>
                setBulkReason(e.target.value)
              }
              placeholder="교대조 변경 / 근무종료 이월 등"
            />
          </label>

          <button
            className="primary-button"
            onClick={() => void bulkTransfer()}
            disabled={
              bulkPending
                || selectedRows.length === 0
            }
          >
            <ArrowRightLeft size={13}/>
            선택 인수인계 실행
          </button>
        </div>

        <p className="hint-copy erp-handover-bulk-note">
          일괄 처리는 최대 50건이며 전체가 하나의 Transaction으로 처리됩니다.
          한 건이라도 담당자나 Session 상태가 바뀌면 일부만 저장하지 않고 전체를 취소합니다.
        </p>
      </Panel>

      <Panel title="미처리 업무">
        <div className="table-wrap erp-handover-table-wrap">
          <table className="erp-handover-table">
            <thead>
              <tr>
                <th className="erp-select-col">
                  <input
                    type="checkbox"
                    checked={allVisibleSelected}
                    disabled={bulkPending}
                    onChange={toggleVisible}
                    aria-label="현재 조회 행 전체 선택"
                  />
                </th>
                <th>Assignment</th>
                <th>운영상태</th>
                <th>현재 MATE</th>
                <th>PDA</th>
                <th>업무 / 구역</th>
                <th>이어갈 위치</th>
                <th>마지막 Session</th>
                <th>빠른 재배정</th>
                <th>원본</th>
              </tr>
            </thead>

            <tbody>
              {rows.map((row) => (
                <tr
                  key={row.assignmentId}
                  className={
                    row.handoverCandidate
                      ? 'handover-candidate-row'
                      : ''
                  }
                >
                  <td className="erp-select-col">
                    <input
                      type="checkbox"
                      checked={selectedIds.includes(
                        row.assignmentId
                      )}
                      disabled={bulkPending}
                      onChange={() =>
                        toggleRow(row.assignmentId)
                      }
                      aria-label={`Assignment ${row.assignmentId} 선택`}
                    />
                  </td>
                  <td>
                    <strong>#{row.assignmentId}</strong>
                    <small>{row.assignmentStatus}</small>
                  </td>

                  <td>
                    <span
                      className={`erp-handover-state state-${row.handoverState.toLowerCase()}`}
                    >
                      {row.stateLabel}
                    </span>

                    {row.mateBusyElsewhere && (
                      <small className="erp-handover-busy">
                        <AlertTriangle size={10}/>
                        현재 다른 업무 Session 진행중
                      </small>
                    )}
                  </td>

                  <td>
                    <strong>
                      {row.currentMateNickname}
                    </strong>
                    <small>{row.employeeNo}</small>
                    <StatusBadge
                      status={row.currentMateStatus}
                    />
                    <small>
                      {row.currentMateWhereabouts ?? '-'}
                    </small>
                  </td>

                  <td>
                    {row.currentPdaNumber == null
                      ? '-'
                      : (
                        <strong>
                          PDA {row.currentPdaNumber}
                        </strong>
                      )}
                  </td>

                  <td>
                    <strong>{row.workType}</strong>
                    <small>
                      {row.area} · 시작 {row.startLocation}
                    </small>
                  </td>

                  <td>
                    <strong>
                      {row.currentLastCompletedLocation
                        ?? row.startLocation}
                    </strong>
                    <small>
                      {row.currentLastCompletedLocation
                        ? '마지막 완료지점 기준'
                        : '수행기록 없음 · 시작점 기준'}
                    </small>
                  </td>

                  <td>
                    {row.lastSessionId == null ? (
                      <span>세션 없음</span>
                    ) : (
                      <>
                        <strong>
                          #{row.lastSessionId}
                          {' · '}
                          {row.lastSessionEndReason ?? '-'}
                        </strong>
                        <small>
                          종료 {stamp(row.lastSessionEndedAt)}
                        </small>
                        <small>
                          {row.lastSessionQuality ?? '-'}
                        </small>
                      </>
                    )}
                  </td>

                  <td>
                    <div className="erp-handover-reassign">
                      <select
                        disabled={bulkPending}
                        value={
                          selectedMate[row.assignmentId]
                            ?? ''
                        }
                        onChange={(e) =>
                          setSelectedMate((prev) => ({
                            ...prev,
                            [row.assignmentId]:
                              e.target.value
                          }))
                        }
                      >
                        <option value="">
                          새 담당 선택
                        </option>
                        {candidates(row).map((mate) => (
                          <option
                            key={mate.id}
                            value={mate.id}
                          >
                            {mate.nickname}
                            {' · '}
                            {mate.status}
                          </option>
                        ))}
                      </select>

                      <input
                        disabled={bulkPending}
                        value={
                          reason[row.assignmentId]
                            ?? ''
                        }
                        onChange={(e) =>
                          setReason((prev) => ({
                            ...prev,
                            [row.assignmentId]:
                              e.target.value
                          }))
                        }
                        placeholder="사유 선택입력"
                      />

                      <button
                        disabled={
                          bulkPending
                            || pendingId === row.assignmentId
                            || !selectedMate[
                              row.assignmentId
                            ]
                        }
                        onClick={() =>
                          void reassign(row)
                        }
                      >
                        <ArrowRightLeft size={12}/>
                        인수인계
                      </button>

                      {!row.handoverCandidate && (
                        <small>
                          현재 담당 유지 시 MATE 업무탭에서 그대로 재개
                        </small>
                      )}
                    </div>
                  </td>

                  <td>
                    <div className="erp-cell-actions vertical">
                      <Link
                        className="erp-row-button"
                        to={`/assignments?assignmentId=${row.assignmentId}`}
                      >
                        <ExternalLink size={12}/>
                        업무
                      </Link>
                      <Link
                        className="erp-row-button"
                        to={`/mates?mateId=${row.currentMateId}`}
                      >
                        <UserRoundCheck size={12}/>
                        MATE
                      </Link>
                    </div>
                  </td>
                </tr>
              ))}

              {rows.length === 0 && (
                <tr>
                  <td
                    colSpan={10}
                    className="empty-cell"
                  >
                    현재 조건에 해당하는 미처리 업무가 없습니다.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Panel>

      <div className="erp-handover-policy">
        <strong>운영 원칙</strong>
        <span>
          이 화면의 순서는 업무 중요도나 자동 우선순위가 아닙니다.
          Open Session이 없는 활성 업무를 운영상태별로 모아 보여주며,
          담당 변경 시 마지막 수행위치와 기존 이력은 그대로 유지됩니다.
        </span>
      </div>
    </div>
  )
}
