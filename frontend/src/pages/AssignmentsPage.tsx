import { Fragment, useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { ArrowRightLeft, History, RefreshCw, RotateCcw, Search, TimerReset, XCircle } from 'lucide-react'
import { api } from '../api/client'
import type {
  Location,
  Mate,
  WorkAssignment,
  WorkAssignmentHistory,
  WorkEstimate,
  WorkProgress,
  WorkSession,
  WorkType
} from '../api/types'
import { Panel } from '../components/Panel'

function stamp(v: string | null) {
  if (!v) return '-'
  return new Intl.DateTimeFormat('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(v))
}

function estimateTime(value: number | null) {
  if (value == null) return '이력 부족'
  const rawMinutes = Math.max(0, Math.round(value / 60))
  const rounded = rawMinutes < 10 ? rawMinutes : Math.round(rawMinutes / 10) * 10
  const hours = Math.floor(rounded / 60)
  const minutes = rounded % 60

  if (hours === 0) return `약 ${minutes}분`
  if (minutes === 0) return `약 ${hours}시간`
  return `약 ${hours}시간 ${minutes}분`
}

function dur(v: number | null) {
  if (v == null) return '-'
  const h = Math.floor(v / 3600)
  const m = Math.floor((v % 3600) / 60)
  const s = v % 60
  return h > 0 ? `${h}시간 ${m}분` : `${m}분 ${s}초`
}

export function AssignmentsPage() {
  const [searchParams] = useSearchParams()
  const [mates, setMates] = useState<Mate[]>([])
  const [locations, setLocations] = useState<Location[]>([])
  const [workTypes, setWorkTypes] = useState<WorkType[]>([])
  const [assignments, setAssignments] = useState<WorkAssignment[]>([])

  const [mateId, setMateId] = useState('')
  const [workTypeId, setWorkTypeId] = useState('')
  const [areaId, setAreaId] = useState('')
  const [startId, setStartId] = useState('')
  const [startSearch, setStartSearch] = useState('')
  const [estimate, setEstimate] = useState<WorkEstimate | null>(null)
  const [estimateLoading, setEstimateLoading] = useState(false)

  const [statusFilter, setStatusFilter] = useState('ALL')
  const [mateFilter, setMateFilter] = useState('')
  const [workFilter, setWorkFilter] = useState('')
  const [areaFilter, setAreaFilter] = useState('')
  const [keyword, setKeyword] = useState('')

  const [message, setMessage] = useState('')
  const [detail, setDetail] = useState<number | null>(null)
  const [progress, setProgress] = useState<WorkProgress[]>([])
  const [assignmentHistory, setAssignmentHistory] = useState<WorkAssignmentHistory[]>([])
  const [sessions, setSessions] = useState<WorkSession[]>([])
  const [tradeId, setTradeId] = useState<number | null>(null)
  const [tradeMateId, setTradeMateId] = useState('')
  const [tradeReason, setTradeReason] = useState('')
  const [correctionId, setCorrectionId] = useState<number | null>(null)
  const [correctionLocationId, setCorrectionLocationId] = useState('')
  const [correctionReason, setCorrectionReason] = useState('')

  const load = async () => {
    const [m, l, w, a] = await Promise.all([
      api.mates(),
      api.locations(),
      api.workTypes(),
      api.workAssignments()
    ])
    setMates(m.filter((x) => x.active))
    setLocations(l.filter((x) => x.active))
    setWorkTypes(w.filter((x) => x.active))
    setAssignments(a)
  }

  useEffect(() => {
    void load()
  }, [])

  useEffect(() => {
    const requestedMateId = searchParams.get('mateId')
    const requestedAssignmentId = searchParams.get('assignmentId')

    if (requestedMateId) {
      setMateId(requestedMateId)
    }

    if (requestedAssignmentId) {
      setKeyword(requestedAssignmentId)
      setStatusFilter('ALL')
    }
  }, [searchParams])

  useEffect(() => {
    if (!areaId || !workTypeId) {
      setEstimate(null)
      return
    }

    let cancelled = false
    setEstimateLoading(true)

    void api.workEstimate(
      Number(areaId),
      Number(workTypeId),
      startId ? Number(startId) : undefined
    )
      .then((data) => {
        if (!cancelled) setEstimate(data)
      })
      .catch(() => {
        if (!cancelled) setEstimate(null)
      })
      .finally(() => {
        if (!cancelled) setEstimateLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [areaId, workTypeId, startId])

  const roots = useMemo(
    () => locations.filter((l) => l.parentId === null),
    [locations]
  )

  const startOptions = useMemo(() => {
    if (!areaId) return []
    const area = locations.find((l) => l.id === Number(areaId))
    if (!area) return []

    return locations.filter((l) =>
      (l.fullCode === area.fullCode || l.fullCode.startsWith(`${area.fullCode}-`))
      && (!startSearch || l.fullCode.includes(startSearch.toUpperCase()))
    )
  }, [areaId, locations, startSearch])

  const filteredAssignments = useMemo(() => {
    const q = keyword.trim().toUpperCase()

    return assignments.filter((a) => {
      if (statusFilter !== 'ALL' && a.status !== statusFilter) return false
      if (mateFilter && a.currentMateId !== Number(mateFilter)) return false
      if (workFilter && a.workTypeId !== Number(workFilter)) return false
      if (areaFilter && a.areaLocationId !== Number(areaFilter)) return false

      if (q) {
        const haystack = [
          a.currentMateNickname,
          a.workTypeName,
          a.areaLocation,
          a.startLocation,
          a.currentLastCompletedLocation ?? '',
          String(a.id)
        ].join(' ').toUpperCase()

        if (!haystack.includes(q)) return false
      }

      return true
    })
  }, [
    assignments,
    statusFilter,
    mateFilter,
    workFilter,
    areaFilter,
    keyword
  ])

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    await api.assignWork({
      mateId: Number(mateId),
      workTypeId: Number(workTypeId),
      areaLocationId: Number(areaId),
      startLocationId: Number(startId)
    })

    setMessage('업무를 배정했습니다.')
    setStartId('')
    setStartSearch('')
    await load()
  }

  const openHistory = async (id: number) => {
    if (detail === id) {
      setDetail(null)
      return
    }

    const [p, s, h] = await Promise.all([
      api.progressHistory(id),
      api.sessionHistory(id),
      api.assignmentHistory(id)
    ])

    setProgress(p)
    setSessions(s)
    setAssignmentHistory(h)
    setDetail(id)
  }

  const cancelAssignment = async (assignment: WorkAssignment) => {
    const reason = window.prompt(
      `업무배정 #${assignment.id} 취소 사유를 입력하세요. (선택)`
    )
    if (reason === null) return

    try {
      await api.cancelWork(assignment.id, reason || undefined)
      setMessage('업무배정을 취소했습니다.')
      await load()
    } catch (e) {
      setMessage(e instanceof Error ? e.message : '업무배정을 취소하지 못했습니다.')
    }
  }

  const trade = async () => {
    if (!tradeId || !tradeMateId) return

    const assignment =
      assignments.find((item) => item.id === tradeId)

    try {
      await api.tradeWork(
        tradeId,
        Number(tradeMateId),
        assignment?.currentMateId,
        tradeReason || undefined
      )

      setTradeId(null)
      setTradeMateId('')
      setTradeReason('')
      setMessage('담당 MATE를 변경했습니다.')
      await load()
    } catch (e) {
      setMessage(
        e instanceof Error
          ? e.message
          : '담당 MATE를 변경하지 못했습니다.'
      )
      await load()
    }
  }

  const undoLatestCorrection = async (
    assignment: WorkAssignment
  ) => {
    const latest =
      progress.length > 0
        ? progress[progress.length - 1]
        : null

    if (
      !latest
        || !latest.correction
        || !latest.previousLocation
        || !assignment.currentLastCompletedLocationId
    ) return

    if (
      !window.confirm(
        `Assignment #${assignment.id}\n\n최근 정정 ${latest.previousLocation} → ${latest.lastCompletedLocation}을 되돌릴까요?\n\n기존 정정 Row를 삭제하지 않고 반대 방향의 새 정정 이력을 추가합니다.`
      )
    ) return

    try {
      await api.undoLatestWorkProgressCorrection(
        assignment.id,
        {
          expectedLatestProgressId: latest.id,
          expectedCurrentLocationId:
            assignment.currentLastCompletedLocationId,
          reason: `최근 정정 되돌리기 · progress #${latest.id}`
        }
      )

      setMessage(
        '최근 진행위치 정정을 되돌렸습니다. 원래 정정 이력도 그대로 보존됩니다.'
      )

      await load()

      const [p, s, h] = await Promise.all([
        api.progressHistory(assignment.id),
        api.sessionHistory(assignment.id),
        api.assignmentHistory(assignment.id)
      ])

      setProgress(p)
      setSessions(s)
      setAssignmentHistory(h)
      setDetail(assignment.id)
    } catch (e) {
      setMessage(
        e instanceof Error
          ? e.message
          : '최근 정정을 되돌리지 못했습니다.'
      )
    }
  }

  const correctionAssignment = correctionId
    ? assignments.find(
        (assignment) => assignment.id === correctionId
      ) ?? null
    : null

  const correctionLocations = useMemo(() => {
    if (!correctionAssignment) return []

    const area = locations.find(
      (location) =>
        location.id
          === correctionAssignment.areaLocationId
    )

    if (!area) return []

    return locations.filter((location) => {
      const inArea =
        location.fullCode === area.fullCode
          || location.fullCode.startsWith(
            `${area.fullCode}-`
          )

      const notBeforeStart =
        location.fullCode
          >= correctionAssignment.startLocation

      return inArea && notBeforeStart
    })
  }, [correctionAssignment, locations])

  const openCorrection = (
    assignment: WorkAssignment
  ) => {
    setCorrectionId(assignment.id)
    setCorrectionLocationId('')
    setCorrectionReason('')
  }

  const submitCorrection = async () => {
    if (
      !correctionAssignment
        || !correctionLocationId
    ) return

    if (
      !window.confirm(
        `Assignment #${correctionAssignment.id}\n\n현재 마지막 수행위치 ${correctionAssignment.currentLastCompletedLocation ?? '미기록'}를 선택한 위치로 정정할까요?\n\n기존 진행기록은 삭제되지 않고 정정 이력이 추가됩니다.`
      )
    ) return

    try {
      await api.correctWorkProgress(
        correctionAssignment.id,
        {
          expectedCurrentLocationId:
            correctionAssignment
              .currentLastCompletedLocationId
              ?? undefined,
          correctedLocationId:
            Number(correctionLocationId),
          reason:
            correctionReason.trim()
              || undefined
        }
      )

      setMessage(
        '마지막 수행위치를 정정했습니다. 기존 기록은 이력으로 유지됩니다.'
      )

      const id = correctionAssignment.id

      setCorrectionId(null)
      setCorrectionLocationId('')
      setCorrectionReason('')

      await load()

      const [p, s, h] = await Promise.all([
        api.progressHistory(id),
        api.sessionHistory(id),
        api.assignmentHistory(id)
      ])

      setProgress(p)
      setSessions(s)
      setAssignmentHistory(h)
      setDetail(id)
    } catch (e) {
      setMessage(
        e instanceof Error
          ? e.message
          : '진행위치를 정정하지 못했습니다.'
      )
    }
  }

  return (
    <div className="assignment-page">
      <div className="page-title-row">
        <div>
          <span className="eyebrow">WORK ASSIGNMENT</span>
          <h2>업무배정</h2>
          <p>종료 범위는 지정하지 않고 구역과 시작점만 배정합니다.</p>
        </div>
      </div>

      {message && <div className="toast-inline">{message}</div>}

      <Panel title="신규 업무배정">
        <form className="assignment-form erp-assignment-form" onSubmit={submit}>
          <label>
            MATE
            <select value={mateId} onChange={(e) => setMateId(e.target.value)} required>
              <option value="">선택</option>
              {mates.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.nickname} ({m.employeeNo})
                </option>
              ))}
            </select>
          </label>

          <label>
            업무
            <select value={workTypeId} onChange={(e) => setWorkTypeId(e.target.value)} required>
              <option value="">선택</option>
              {workTypes.map((w) => (
                <option key={w.id} value={w.id}>{w.name}</option>
              ))}
            </select>
          </label>

          <label>
            구역
            <select
              value={areaId}
              onChange={(e) => {
                setAreaId(e.target.value)
                setStartId('')
                setStartSearch('')
              }}
              required
            >
              <option value="">선택</option>
              {roots.map((l) => (
                <option key={l.id} value={l.id}>{l.fullCode}</option>
              ))}
            </select>
          </label>

          <label>
            시작 위치 검색
            <input
              value={startSearch}
              onChange={(e) => setStartSearch(e.target.value.toUpperCase())}
              placeholder="A01-13"
              disabled={!areaId}
            />
          </label>

          <label>
            시작 로케이션
            <select
              value={startId}
              onChange={(e) => setStartId(e.target.value)}
              required
              disabled={!areaId}
            >
              <option value="">선택</option>
              {startOptions.map((l) => (
                <option key={l.id} value={l.id}>{l.fullCode}</option>
              ))}
            </select>
          </label>

          <button className="primary-button">배정</button>
        </form>

        <div className="erp-estimate-strip">
          <div>
            <span>현재 구역 위치</span>
            <strong>
              {estimate
                ? `${estimate.currentProgressPercent}% · ${estimate.currentLastCompletedLocation ?? '수행기록 없음'}`
                : '-'}
            </strong>
          </div>

          <div>
            <span>현재점 기준 예상 잔여</span>
            <strong>
              {estimateLoading
                ? '계산 중'
                : estimate
                  ? estimateTime(estimate.estimatedRemainingFromCurrentSeconds)
                  : '-'}
            </strong>
          </div>

          <div>
            <span>선택 시작점 기준</span>
            <strong>
              {estimate?.selectedStartLocation
                ? `${estimate.selectedStartLocation} (${estimate.selectedStartPercent ?? 0}% 지점) · ${estimateTime(estimate.estimatedRemainingFromSelectedStartSeconds)}`
                : '시작 위치 선택 전'}
            </strong>
          </div>

          <div>
            <span>동일 구역·업무 이력</span>
            <strong>
              {estimate
                ? estimate.historicalSampleCount > 0
                  ? `${estimate.historicalSampleCount}건 · 구역 환산 ${estimateTime(estimate.estimatedFullAreaSeconds)}`
                  : '정상 완료 이력 부족'
                : '-'}
            </strong>
          </div>
        </div>
        <p className="hint-copy erp-estimate-note">
          예상시간은 동일 구역·동일 업무의 정상 종료 WorkSession과 실제 진행구간을 환산한 참고값입니다. 자동 우선순위나 완료 의무를 만들지는 않습니다.
        </p>
      </Panel>

      <Panel
        title="업무배정 조회"
        action={
          <button className="secondary-button compact" onClick={() => void load()}>
            <RefreshCw size={13} /> 새로고침
          </button>
        }
      >
        <div className="erp-filter-toolbar">
          <label>
            상태
            <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
              <option value="ALL">전체</option>
              <option value="ASSIGNED">ASSIGNED</option>
              <option value="IN_PROGRESS">IN_PROGRESS</option>
              <option value="COMPLETED">COMPLETED</option>
              <option value="CANCELED">CANCELED</option>
            </select>
          </label>

          <label>
            MATE
            <select value={mateFilter} onChange={(e) => setMateFilter(e.target.value)}>
              <option value="">전체</option>
              {mates.map((m) => <option key={m.id} value={m.id}>{m.nickname}</option>)}
            </select>
          </label>

          <label>
            업무
            <select value={workFilter} onChange={(e) => setWorkFilter(e.target.value)}>
              <option value="">전체</option>
              {workTypes.map((w) => <option key={w.id} value={w.id}>{w.name}</option>)}
            </select>
          </label>

          <label>
            구역
            <select value={areaFilter} onChange={(e) => setAreaFilter(e.target.value)}>
              <option value="">전체</option>
              {roots.map((r) => <option key={r.id} value={r.id}>{r.fullCode}</option>)}
            </select>
          </label>

          <label className="erp-keyword-filter">
            검색
            <span>
              <Search size={13} />
              <input
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                placeholder="MATE / 위치 / 업무 / 번호"
              />
            </span>
          </label>

          <div className="erp-filter-count">
            조회 {filteredAssignments.length} / 전체 {assignments.length}
          </div>
        </div>

        <div className="table-wrap assignment-table-wrap">
          <table className="erp-assignment-table">
            <thead>
              <tr>
                <th>No.</th>
                <th>MATE</th>
                <th>업무</th>
                <th>구역</th>
                <th>시작</th>
                <th>마지막 수행</th>
                <th>배정일시</th>
                <th>상태</th>
                <th>관리</th>
              </tr>
            </thead>
            <tbody>
              {filteredAssignments.map((a) => (
                <Fragment key={a.id}>
                  <tr className={detail === a.id ? 'selected-row' : ''}>
                    <td>#{a.id}</td>
                    <td><strong>{a.currentMateNickname}</strong></td>
                    <td>{a.workTypeName}</td>
                    <td>{a.areaLocation}</td>
                    <td>{a.startLocation}</td>
                    <td>{a.currentLastCompletedLocation ?? '-'}</td>
                    <td>{stamp(a.assignedAt)}</td>
                    <td><span className={`erp-state-text state-${a.status.toLowerCase()}`}>{a.status}</span></td>
                    <td>
                      <div className="erp-cell-actions">
                        {a.status !== 'COMPLETED' && a.status !== 'CANCELED' && (
                          <>
                            <button
                              title="담당 MATE 변경"
                              onClick={() => {
                                setTradeId(a.id)
                                setTradeMateId('')
                              }}
                            >
                              <ArrowRightLeft size={13} /> 변경
                            </button>
                            <button
                              title="업무배정 취소"
                              onClick={() => void cancelAssignment(a)}
                            >
                              <XCircle size={13} /> 취소
                            </button>
                          </>
                        )}
                        <button title="수행 이력" onClick={() => void openHistory(a.id)}>
                          <History size={13} /> 이력
                        </button>
                      </div>
                    </td>
                  </tr>

                  {detail === a.id && (
                    <tr className="erp-detail-row">
                      <td colSpan={9}>
                        <div className="assignment-history erp-assignment-history">
                          <section>
                            <div className="erp-history-section-head">
                              <h4>진행기록</h4>
                              {(() => {
                                const assignment = assignments.find(
                                  (item) => item.id === a.id
                                )
                                const hasOpenSession = sessions.some(
                                  (session) => session.endedAt === null
                                )

                                if (
                                  !assignment
                                    || assignment.status === 'CANCELED'
                                ) return null

                                const latest =
                                  progress.length > 0
                                    ? progress[progress.length - 1]
                                    : null

                                const canUndo =
                                  Boolean(
                                    latest?.correction
                                      && latest.previousLocation
                                      && assignment
                                        .currentLastCompletedLocationId
                                  )

                                return (
                                  <div className="erp-cell-actions">
                                    {canUndo && (
                                      <button
                                        className="erp-row-button"
                                        disabled={hasOpenSession}
                                        title={
                                          hasOpenSession
                                            ? '작업 중 세션을 먼저 일시정지해야 되돌릴 수 있습니다.'
                                            : '가장 최근 정정 이력을 안전하게 되돌립니다.'
                                        }
                                        onClick={() =>
                                          void undoLatestCorrection(
                                            assignment
                                          )
                                        }
                                      >
                                        <History size={12}/>
                                        최근 정정 되돌리기
                                      </button>
                                    )}

                                    <button
                                      className="erp-row-button"
                                      disabled={hasOpenSession}
                                      title={
                                        hasOpenSession
                                          ? '작업 중 세션을 먼저 일시정지해야 정정할 수 있습니다.'
                                          : '마지막 수행위치 정정'
                                      }
                                      onClick={() =>
                                        openCorrection(assignment)
                                      }
                                    >
                                      <RotateCcw size={12}/>
                                      위치 정정
                                    </button>
                                  </div>
                                )
                              })()}
                            </div>
                            {progress.length === 0 ? (
                              <div className="empty">진행기록 없음</div>
                            ) : (
                              <div className="table-wrap">
                                <table>
                                  <thead>
                                    <tr><th>보고시각</th><th>수행 MATE</th><th>기록 Account</th><th>마지막 위치</th><th>구분</th><th>사유</th></tr>
                                  </thead>
                                  <tbody>
                                    {progress.map((item) => (
                                      <tr
                                        key={item.id}
                                        className={
                                          item.correction
                                            ? 'erp-progress-correction-row'
                                            : ''
                                        }
                                      >
                                        <td>{stamp(item.reportedAt)}</td>
                                        <td>{item.mateNickname}</td>
                                        <td>
                                          <strong>
                                            {item.reportedBy ?? item.mateNickname}
                                          </strong>
                                        </td>
                                        <td>
                                          <strong>
                                            {item.previousLocation
                                              ? `${item.previousLocation} → ${item.lastCompletedLocation}`
                                              : item.lastCompletedLocation}
                                          </strong>
                                        </td>
                                        <td>{item.correction ? '정정' : '진행보고'}</td>
                                        <td>{item.reason ?? '-'}</td>
                                      </tr>
                                    ))}
                                  </tbody>
                                </table>
                              </div>
                            )}
                          </section>

                          <section>
                            <h4>작업 세션</h4>
                            {sessions.length === 0 ? (
                              <div className="empty">작업시간 없음</div>
                            ) : (
                              <div className="table-wrap">
                                <table>
                                  <thead>
                                    <tr><th>근무 기준일</th><th>시작</th><th>종료</th><th>실작업시간</th><th>종료사유</th><th>신뢰도</th></tr>
                                  </thead>
                                  <tbody>
                                    {sessions.map((session) => (
                                      <tr key={session.id}>
                                        <td>
                                          <strong>
                                            {session.shiftDate ?? session.startedAt.slice(0, 10)}
                                          </strong>
                                        </td>
                                        <td>{stamp(session.startedAt)}</td>
                                        <td>{stamp(session.endedAt)}</td>
                                        <td><strong>{dur(session.durationSeconds)}</strong></td>
                                        <td>{session.endReason ?? 'OPEN'}</td>
                                        <td>{session.qualityStatus}</td>
                                      </tr>
                                    ))}
                                  </tbody>
                                </table>
                              </div>
                            )}
                          </section>

                          <section className="erp-history-lifecycle">
                            <h4>배정 변경 이력</h4>
                            {assignmentHistory.length === 0 ? (
                              <div className="empty">배정 변경이력 없음</div>
                            ) : (
                              <div className="table-wrap">
                                <table>
                                  <thead>
                                    <tr><th>시각</th><th>구분</th><th>이전 MATE</th><th>새 MATE</th><th>처리자</th><th>사유</th></tr>
                                  </thead>
                                  <tbody>
                                    {assignmentHistory.map((item) => (
                                      <tr key={item.id}>
                                        <td>{stamp(item.changedAt)}</td>
                                        <td><strong>{item.actionType}</strong></td>
                                        <td>{item.fromMateNickname ?? '-'}</td>
                                        <td>{item.toMateNickname ?? '-'}</td>
                                        <td>{item.actor}</td>
                                        <td>{item.reason ?? '-'}</td>
                                      </tr>
                                    ))}
                                  </tbody>
                                </table>
                              </div>
                            )}
                          </section>
                        </div>
                      </td>
                    </tr>
                  )}
                </Fragment>
              ))}

              {filteredAssignments.length === 0 && (
                <tr>
                  <td colSpan={9} className="empty-cell">조건에 맞는 업무배정이 없습니다.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Panel>

      {correctionAssignment && (
        <div
          className="modal-backdrop"
          onMouseDown={() =>
            setCorrectionId(null)
          }
        >
          <div
            className="modal-card erp-modal-card"
            onMouseDown={(e) =>
              e.stopPropagation()
            }
          >
            <div className="erp-modal-head">
              <strong>마지막 수행위치 정정</strong>
              <span>
                ASSIGNMENT #{correctionAssignment.id}
              </span>
            </div>

            <div className="erp-modal-body">
              <div className="erp-correction-current">
                <span>현재 저장값</span>
                <strong>
                  {correctionAssignment
                    .currentLastCompletedLocation
                    ?? '미기록'}
                </strong>
              </div>

              <label>
                정정할 로케이션
                <select
                  value={correctionLocationId}
                  onChange={(e) =>
                    setCorrectionLocationId(
                      e.target.value
                    )
                  }
                >
                  <option value="">선택</option>
                  {correctionLocations.map(
                    (location) => (
                      <option
                        key={location.id}
                        value={location.id}
                        disabled={
                          location.id
                            === correctionAssignment
                              .currentLastCompletedLocationId
                        }
                      >
                        {location.fullCode}
                      </option>
                    )
                  )}
                </select>
              </label>

              <label>
                정정 사유 <small>선택</small>
                <textarea
                  value={correctionReason}
                  onChange={(e) =>
                    setCorrectionReason(
                      e.target.value
                    )
                  }
                  placeholder="오입력 정정 / 현장 확인 등"
                />
              </label>

              <div className="erp-correction-warning">
                저장 직전에 현재 마지막 수행위치를 다시 검증합니다.
                다른 화면에서 먼저 진행값이 변경되었다면 정정을 중단하고
                최신 이력을 다시 확인하게 합니다.
              </div>
            </div>

            <div className="modal-actions">
              <button
                className="secondary-button"
                onClick={() =>
                  setCorrectionId(null)
                }
              >
                취소
              </button>
              <button
                className="primary-button"
                disabled={!correctionLocationId}
                onClick={() =>
                  void submitCorrection()
                }
              >
                <RotateCcw size={14}/>
                정정 저장
              </button>
            </div>
          </div>
        </div>
      )}

      {tradeId && (
        <div className="modal-backdrop" onMouseDown={() => setTradeId(null)}>
          <div className="modal-card erp-modal-card" onMouseDown={(e) => e.stopPropagation()}>
            <div className="erp-modal-head">
              <strong>담당 MATE 변경</strong>
              <span>ASSIGNMENT #{tradeId}</span>
            </div>

            <div className="erp-modal-body">
              <label>
                새 담당
                <select value={tradeMateId} onChange={(e) => setTradeMateId(e.target.value)}>
                  <option value="">선택</option>
                  {mates.map((m) => (
                    <option key={m.id} value={m.id}>{m.nickname}</option>
                  ))}
                </select>
              </label>

              <label>
                변경 사유 <small>선택</small>
                <textarea
                  value={tradeReason}
                  onChange={(e) => setTradeReason(e.target.value)}
                />
              </label>

              <p className="hint-copy">
                진행 중인 WorkSession이 있으면 먼저 일시정지해야 담당자를 변경할 수 있습니다.
              </p>
            </div>

            <div className="modal-actions">
              <button className="secondary-button" onClick={() => setTradeId(null)}>취소</button>
              <button className="primary-button" onClick={() => void trade()} disabled={!tradeMateId}>
                <TimerReset size={14} /> 변경
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
