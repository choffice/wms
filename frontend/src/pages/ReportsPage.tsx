import { useEffect, useMemo, useState } from 'react'
import {
  CalendarDays,
  Clock3,
  FileBarChart,
  RefreshCw,
  Search
} from 'lucide-react'
import { useSearchParams } from 'react-router-dom'
import { api } from '../api/client'
import type {
  DailyReport,
  Mate,
  RangeReport,
  ShiftReport,
  WorkTimeStat,
  WorkType
} from '../api/types'
import { Panel } from '../components/Panel'

function localDateInput(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const now = new Date()
const today = localDateInput(now)
const weekAgoDate = new Date(now)
weekAgoDate.setDate(weekAgoDate.getDate() - 6)
const weekAgo = localDateInput(weekAgoDate)

function seconds(value: number) {
  const h = Math.floor(value / 3600)
  const m = Math.floor((value % 3600) / 60)
  return h > 0 ? `${h}시간 ${m}분` : `${m}분`
}

function stamp(value: string | null) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('ko-KR', {
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

function dayLabel(value: string) {
  return new Intl.DateTimeFormat('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    weekday: 'short'
  }).format(new Date(`${value}T00:00:00`))
}

export function ReportsPage() {
  const [searchParams] = useSearchParams()

  const [mode, setMode] = useState<'DAILY'|'SHIFT'|'RANGE'>(
    () => {
      const requested = searchParams.get('mode')
      return requested === 'SHIFT'
        || requested === 'RANGE'
        ? requested
        : 'DAILY'
    }
  )

  const [date, setDate] = useState(today)
  const [report, setReport] = useState<DailyReport | null>(null)
  const [shiftDate, setShiftDate] = useState(
    () => searchParams.get('shiftDate') ?? today
  )
  const [shiftReport, setShiftReport] = useState<ShiftReport | null>(null)

  const [stats, setStats] = useState<WorkTimeStat[]>([])
  const [mates, setMates] = useState<Mate[]>([])
  const [workTypes, setWorkTypes] = useState<WorkType[]>([])
  const [mateId, setMateId] = useState('')
  const [workTypeId, setWorkTypeId] = useState('')
  const [includeUncertain, setIncludeUncertain] = useState(false)

  const [from, setFrom] = useState(weekAgo)
  const [to, setTo] = useState(today)
  const [range, setRange] = useState<RangeReport | null>(null)
  const [rangeKeyword, setRangeKeyword] = useState('')
  const [rangeArea, setRangeArea] = useState('ALL')
  const [rangeWorkType, setRangeWorkType] = useState('ALL')

  const [message, setMessage] = useState('')

  const loadDaily = async () => {
    setReport(await api.dailyReport(date))
  }

  const loadShift = async () => {
    setMessage('')
    try {
      setShiftReport(
        await api.shiftReport(shiftDate)
      )
    } catch (e) {
      setMessage(
        e instanceof Error
          ? e.message
          : '근무조 보고서를 불러오지 못했습니다.'
      )
    }
  }

  const loadStats = async () => {
    setStats(
      await api.workTimeStats({
        from: date,
        to: date,
        mateId: mateId ? Number(mateId) : undefined,
        workTypeId: workTypeId ? Number(workTypeId) : undefined,
        includeUncertain
      })
    )
  }

  const loadRange = async () => {
    setMessage('')
    try {
      setRange(await api.rangeReport(from, to))
    } catch (e) {
      setMessage(
        e instanceof Error
          ? e.message
          : '기간 통계를 불러오지 못했습니다.'
      )
    }
  }

  useEffect(() => {
    void Promise.all([
      api.mates(),
      api.workTypes()
    ]).then(([m, w]) => {
      setMates(m)
      setWorkTypes(w)
    })
  }, [])

  useEffect(() => {
    if (mode !== 'DAILY') return
    void loadDaily()
    void loadStats()
  }, [
    mode,
    date,
    mateId,
    workTypeId,
    includeUncertain
  ])

  useEffect(() => {
    const requestedMode =
      searchParams.get('mode')
    const requestedShiftDate =
      searchParams.get('shiftDate')

    if (
      requestedMode === 'DAILY'
        || requestedMode === 'SHIFT'
        || requestedMode === 'RANGE'
    ) {
      setMode(requestedMode)
    }

    if (requestedShiftDate) {
      setShiftDate(requestedShiftDate)
    }
  }, [searchParams])

  useEffect(() => {
    if (mode !== 'SHIFT') return
    void loadShift()
  }, [mode, shiftDate])

  useEffect(() => {
    if (mode !== 'RANGE') return
    void loadRange()
  }, [mode, from, to])

  const rangeAreas = useMemo(
    () => range
      ? [...new Set(range.areaWorks.map((row) => row.area))]
      : [],
    [range]
  )

  const rangeWorkTypes = useMemo(
    () => range
      ? [...new Set(range.areaWorks.map((row) => row.workType))]
      : [],
    [range]
  )

  const filteredMateRows = useMemo(() => {
    if (!range) return []
    const q = rangeKeyword.trim().toUpperCase()

    return range.mates.filter((row) => {
      if (!q) return true
      return [
        row.employeeNo,
        row.nickname
      ].join(' ').toUpperCase().includes(q)
    })
  }, [range, rangeKeyword])

  const filteredAreaRows = useMemo(() => {
    if (!range) return []

    return range.areaWorks.filter((row) => {
      if (
        rangeArea !== 'ALL'
          && row.area !== rangeArea
      ) return false

      if (
        rangeWorkType !== 'ALL'
          && row.workType !== rangeWorkType
      ) return false

      return true
    })
  }, [range, rangeArea, rangeWorkType])

  return (
    <div className="stack-page">
      <div className="page-title-row">
        <div>
          <span className="eyebrow">REPORT & DATA</span>
          <h2>보고서 / 통계</h2>
          <p>
            달력일 보고서와 shiftDate 근무조 보고서를 분리해 실제 작업시간·특이사항·PDA 기록을 집계합니다.
          </p>
        </div>
      </div>

      {message && (
        <div className="toast-inline">{message}</div>
      )}

      <div className="settings-tabs erp-report-tabs">
        <button
          className={mode === 'DAILY' ? 'active' : ''}
          onClick={() => setMode('DAILY')}
        >
          <CalendarDays size={13}/>
          일일 보고서(달력일)
        </button>
        <button
          className={mode === 'RANGE' ? 'active' : ''}
          onClick={() => setMode('RANGE')}
        >
          <FileBarChart size={13}/>
          기간 통계(달력일)
        </button>
      </div>

      {mode === 'DAILY' ? (
        <>
          <Panel title="일일 조회 조건">
            <div className="report-filter erp-report-filter">
              <label>
                일자
                <input
                  type="date"
                  value={date}
                  onChange={(e) => setDate(e.target.value)}
                />
              </label>

              <label>
                MATE
                <select
                  value={mateId}
                  onChange={(e) => setMateId(e.target.value)}
                >
                  <option value="">전체</option>
                  {mates.map((mate) => (
                    <option key={mate.id} value={mate.id}>
                      {mate.nickname}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                업무
                <select
                  value={workTypeId}
                  onChange={(e) => setWorkTypeId(e.target.value)}
                >
                  <option value="">전체</option>
                  {workTypes.map((workType) => (
                    <option
                      key={workType.id}
                      value={workType.id}
                    >
                      {workType.name}
                    </option>
                  ))}
                </select>
              </label>

              <label className="inline-check erp-report-check">
                <input
                  type="checkbox"
                  checked={includeUncertain}
                  onChange={(e) =>
                    setIncludeUncertain(e.target.checked)
                  }
                />
                UNCERTAIN 포함
              </label>

              <button
                className="secondary-button"
                onClick={() =>
                  void Promise.all([
                    loadDaily(),
                    loadStats()
                  ])
                }
              >
                <RefreshCw size={13}/>
                갱신
              </button>
            </div>
          </Panel>

          <Panel title="업무 종류별 작업시간">
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>업무 종류</th>
                    <th>세션 수</th>
                    <th>총 실제 작업시간</th>
                    <th>평균 실제 작업시간</th>
                  </tr>
                </thead>
                <tbody>
                  {stats.map((stat) => (
                    <tr key={stat.workType}>
                      <td><strong>{stat.workType}</strong></td>
                      <td>{stat.sessionCount}</td>
                      <td>{seconds(stat.totalSeconds)}</td>
                      <td>
                        <strong>
                          {seconds(stat.averageSeconds)}
                        </strong>
                      </td>
                    </tr>
                  ))}

                  {stats.length === 0 && (
                    <tr>
                      <td colSpan={4} className="empty-cell">
                        해당 조건의 종료된 작업시간 데이터가 없습니다.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>

            <p className="hint-copy erp-report-note">
              일자 경계를 걸친 WorkSession은 조회 일자에 실제로 겹친 시간만 계산합니다.
            </p>
          </Panel>

          <Panel title={`${date} 일일 보고서`}>
            {!report ? (
              <div className="empty">조회 중…</div>
            ) : (
              <div className="report-sections erp-report-sections">
                <section>
                  <h3>
                    진행 업무
                    <span>{report.works.length}건</span>
                  </h3>

                  <div className="table-wrap">
                    <table>
                      <thead>
                        <tr>
                          <th>Assignment</th>
                          <th>MATE</th>
                          <th>PDA</th>
                          <th>업무</th>
                          <th>구역</th>
                          <th>시작</th>
                          <th>마지막 수행</th>
                          <th>작업시간</th>
                          <th>신뢰도</th>
                        </tr>
                      </thead>
                      <tbody>
                        {report.works.map((row) => (
                          <tr
                            key={`${row.assignmentId}-${row.mateNickname}`}
                          >
                            <td>#{row.assignmentId}</td>
                            <td><strong>{row.mateNickname}</strong></td>
                            <td>{row.pdaNumber}</td>
                            <td>{row.workType}</td>
                            <td>{row.area}</td>
                            <td>{row.startLocation}</td>
                            <td>
                              {row.lastCompletedLocation ?? '-'}
                            </td>
                            <td>
                              <strong>
                                {seconds(row.actualWorkSeconds)}
                              </strong>
                            </td>
                            <td>{row.qualityStatus}</td>
                          </tr>
                        ))}

                        {report.works.length === 0 && (
                          <tr>
                            <td colSpan={9} className="empty-cell">
                              작업기록이 없습니다.
                            </td>
                          </tr>
                        )}
                      </tbody>
                    </table>
                  </div>
                </section>

                <section>
                  <h3>
                    PDA 사용
                    <span>{report.pdaUsages.length}건</span>
                  </h3>

                  <div className="table-wrap">
                    <table>
                      <thead>
                        <tr>
                          <th>PDA</th>
                          <th>MATE</th>
                          <th>사용 시작</th>
                          <th>반납</th>
                          <th>사유</th>
                        </tr>
                      </thead>
                      <tbody>
                        {report.pdaUsages.map((row, index) => (
                          <tr key={`${row.pdaNumber}-${index}`}>
                            <td>
                              <strong>PDA {row.pdaNumber}</strong>
                            </td>
                            <td>{row.mateNickname}</td>
                            <td>{stamp(row.assignedAt)}</td>
                            <td>{stamp(row.releasedAt)}</td>
                            <td>{row.releaseReason ?? '-'}</td>
                          </tr>
                        ))}

                        {report.pdaUsages.length === 0 && (
                          <tr>
                            <td colSpan={5} className="empty-cell">
                              PDA 사용기록이 없습니다.
                            </td>
                          </tr>
                        )}
                      </tbody>
                    </table>
                  </div>
                </section>

                <section>
                  <h3>
                    특이사항
                    <span>{report.issues.length}건</span>
                  </h3>

                  <div className="table-wrap">
                    <table>
                      <thead>
                        <tr>
                          <th>No.</th>
                          <th>구분</th>
                          <th>작성자</th>
                          <th>로케이션</th>
                          <th>내용</th>
                          <th>상태</th>
                        </tr>
                      </thead>
                      <tbody>
                        {report.issues.map((row) => (
                          <tr key={row.issueId}>
                            <td>#{row.issueId}</td>
                            <td>{row.issueType}</td>
                            <td>{row.authorNickname}</td>
                            <td>{row.location ?? '-'}</td>
                            <td>{row.comment}</td>
                            <td>{row.status}</td>
                          </tr>
                        ))}

                        {report.issues.length === 0 && (
                          <tr>
                            <td colSpan={6} className="empty-cell">
                              특이사항 등록기록이 없습니다.
                            </td>
                          </tr>
                        )}
                      </tbody>
                    </table>
                  </div>
                </section>
              </div>
            )}
          </Panel>
        </>
      ) : mode === 'SHIFT' ? (
        <>
          <Panel title="근무조 조회 조건">
            <div className="report-filter erp-report-filter">
              <label>
                근무조 기준일
                <input
                  type="date"
                  value={shiftDate}
                  onChange={(e) =>
                    setShiftDate(e.target.value)
                  }
                />
              </label>

              <button
                className="secondary-button"
                onClick={() => void loadShift()}
              >
                <RefreshCw size={13}/>
                갱신
              </button>

              <small className="erp-shift-report-help">
                야간조는 시작일을 기준일로 사용합니다.
                예: 08/27 22:00 → 08/28 06:00은 08/27 근무조입니다.
              </small>
            </div>
          </Panel>

          {!shiftReport ? (
            <Panel title="근무조 보고서">
              <div className="empty">조회 중…</div>
            </Panel>
          ) : (
            <>
              <div className="erp-report-summary erp-shift-report-summary">
                <div>
                  <span>실제 작업시간</span>
                  <strong>
                    {seconds(
                      shiftReport.summary.actualWorkSeconds
                    )}
                  </strong>
                </div>
                <div>
                  <span>세션</span>
                  <strong>
                    {shiftReport.summary.sessionCount}
                  </strong>
                </div>
                <div
                  className={
                    shiftReport.summary.openSessionCount
                      ? 'warn'
                      : ''
                  }
                >
                  <span>Open Session</span>
                  <strong>
                    {shiftReport.summary.openSessionCount}
                  </strong>
                </div>
                <div
                  className={
                    shiftReport.summary.uncertainSessionCount
                      ? 'warn'
                      : ''
                  }
                >
                  <span>UNCERTAIN</span>
                  <strong>
                    {shiftReport.summary.uncertainSessionCount}
                  </strong>
                </div>
                <div>
                  <span>Assignment</span>
                  <strong>
                    {shiftReport.summary.assignmentCount}
                  </strong>
                </div>
                <div>
                  <span>MATE</span>
                  <strong>
                    {shiftReport.summary.mateCount}
                  </strong>
                </div>
                <div>
                  <span>특이사항</span>
                  <strong>
                    {shiftReport.summary.issueCount}
                  </strong>
                </div>
                <div>
                  <span>야간조 세션</span>
                  <strong>
                    {shiftReport.summary.overnightSessionCount}
                  </strong>
                </div>
              </div>

              <Panel title="이전 근무조 대비">
                <div className="erp-shift-compare">
                  <div>
                    <span>이전 근무조</span>
                    <strong>
                      {shiftReport.comparison.previousShiftDate
                        ?? '비교 이력 없음'}
                    </strong>
                  </div>
                  <div>
                    <span>이전 작업시간</span>
                    <strong>
                      {seconds(
                        shiftReport.comparison.previousWorkSeconds
                      )}
                    </strong>
                  </div>
                  <div>
                    <span>작업시간 증감</span>
                    <strong>
                      {shiftReport.comparison.previousShiftDate
                        ? (
                          <>
                            {shiftReport.comparison.workSecondsDelta >= 0
                              ? '+'
                              : '-'}
                            {seconds(
                              Math.abs(
                                shiftReport.comparison.workSecondsDelta
                              )
                            )}
                          </>
                        )
                        : '-'}
                    </strong>
                  </div>
                  <div>
                    <span>특이사항 증감</span>
                    <strong>
                      {shiftReport.comparison.previousShiftDate
                        ? `${shiftReport.comparison.issueCountDelta >= 0 ? '+' : ''}${shiftReport.comparison.issueCountDelta}`
                        : '-'}
                    </strong>
                  </div>
                </div>
              </Panel>

              <Panel title={`${shiftReport.shiftDate} 근무조 작업`}>
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>Assignment</th>
                        <th>MATE</th>
                        <th>PDA</th>
                        <th>업무</th>
                        <th>구역</th>
                        <th>시작</th>
                        <th>마지막 수행</th>
                        <th>작업시간</th>
                        <th>신뢰도</th>
                      </tr>
                    </thead>
                    <tbody>
                      {shiftReport.works.map((row) => (
                        <tr
                          key={`${row.assignmentId}-${row.mateNickname}`}
                        >
                          <td>#{row.assignmentId}</td>
                          <td><strong>{row.mateNickname}</strong></td>
                          <td>{row.pdaNumber}</td>
                          <td>{row.workType}</td>
                          <td>{row.area}</td>
                          <td>{row.startLocation}</td>
                          <td>
                            {row.lastCompletedLocation ?? '-'}
                          </td>
                          <td>
                            <strong>
                              {seconds(row.actualWorkSeconds)}
                            </strong>
                          </td>
                          <td>{row.qualityStatus}</td>
                        </tr>
                      ))}

                      {shiftReport.works.length === 0 && (
                        <tr>
                          <td colSpan={9} className="empty-cell">
                            해당 근무조의 작업기록이 없습니다.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>

                <p className="hint-copy erp-report-note">
                  자정을 넘긴 WorkSession도 시간 전체를 시작일의 shiftDate에 귀속합니다.
                  달력 자정에서 작업시간을 둘로 나누지 않습니다.
                </p>
              </Panel>

              <div className="erp-handover-overview-grid">
                <Panel title={`특이사항 · ${shiftReport.issues.length}건`}>
                  <div className="table-wrap">
                    <table>
                      <thead>
                        <tr>
                          <th>No.</th>
                          <th>구분</th>
                          <th>작성자</th>
                          <th>위치</th>
                          <th>내용</th>
                          <th>상태</th>
                        </tr>
                      </thead>
                      <tbody>
                        {shiftReport.issues.map((row) => (
                          <tr key={row.issueId}>
                            <td>#{row.issueId}</td>
                            <td>{row.issueType}</td>
                            <td>{row.authorNickname}</td>
                            <td>{row.location ?? '-'}</td>
                            <td>{row.comment}</td>
                            <td>{row.status}</td>
                          </tr>
                        ))}
                        {shiftReport.issues.length === 0 && (
                          <tr>
                            <td colSpan={6} className="empty-cell">
                              해당 근무조 특이사항이 없습니다.
                            </td>
                          </tr>
                        )}
                      </tbody>
                    </table>
                  </div>
                </Panel>

                <Panel title={`PDA 사용 · ${shiftReport.pdaUsages.length}건`}>
                  <div className="table-wrap">
                    <table>
                      <thead>
                        <tr>
                          <th>PDA</th>
                          <th>MATE</th>
                          <th>사용 시작</th>
                          <th>반납</th>
                          <th>사유</th>
                        </tr>
                      </thead>
                      <tbody>
                        {shiftReport.pdaUsages.map((row, index) => (
                          <tr key={`${row.pdaNumber}-${index}`}>
                            <td><strong>PDA {row.pdaNumber}</strong></td>
                            <td>{row.mateNickname}</td>
                            <td>{stamp(row.assignedAt)}</td>
                            <td>{stamp(row.releasedAt)}</td>
                            <td>{row.releaseReason ?? '-'}</td>
                          </tr>
                        ))}
                        {shiftReport.pdaUsages.length === 0 && (
                          <tr>
                            <td colSpan={5} className="empty-cell">
                              해당 근무조 PDA 사용이력이 없습니다.
                            </td>
                          </tr>
                        )}
                      </tbody>
                    </table>
                  </div>
                </Panel>
              </div>
            </>
          )}
        </>
      ) : (
        <>
          <Panel title="기간 조회 조건">
            <div className="erp-range-filter">
              <label>
                시작일
                <input
                  type="date"
                  value={from}
                  onChange={(e) => setFrom(e.target.value)}
                />
              </label>

              <label>
                종료일
                <input
                  type="date"
                  value={to}
                  onChange={(e) => setTo(e.target.value)}
                />
              </label>

              <button
                className="secondary-button"
                onClick={() => void loadRange()}
              >
                <RefreshCw size={13}/>
                조회
              </button>

              <small>
                최대 366일 · 종료된 WorkSession 기준
              </small>
            </div>
          </Panel>

          {!range ? (
            <Panel title="기간 통계">
              <div className="empty">조회 중…</div>
            </Panel>
          ) : (
            <>
              <div className="erp-report-summary">
                <div>
                  <span>정상 작업시간</span>
                  <strong>
                    {seconds(range.summary.normalSeconds)}
                  </strong>
                </div>
                <div
                  className={
                    range.summary.uncertainSeconds
                      ? 'warn'
                      : ''
                  }
                >
                  <span>UNCERTAIN 시간</span>
                  <strong>
                    {seconds(range.summary.uncertainSeconds)}
                  </strong>
                </div>
                <div>
                  <span>작업세션</span>
                  <strong>{range.summary.sessionCount}</strong>
                </div>
                <div>
                  <span>Assignment</span>
                  <strong>{range.summary.assignmentCount}</strong>
                </div>
                <div>
                  <span>작업 MATE</span>
                  <strong>{range.summary.mateCount}</strong>
                </div>
                <div>
                  <span>특이사항</span>
                  <strong>{range.summary.issueCount}</strong>
                </div>
                <div>
                  <span>PDA 사용이력</span>
                  <strong>{range.summary.pdaUsageCount}</strong>
                </div>
              </div>

              <Panel title="일자별 작업 추이">
                <div className="table-wrap erp-trend-table">
                  <table>
                    <thead>
                      <tr>
                        <th>일자</th>
                        <th>정상 작업시간</th>
                        <th>UNCERTAIN 시간</th>
                        <th>특이사항 등록</th>
                      </tr>
                    </thead>
                    <tbody>
                      {range.dailyTrend.map((row) => (
                        <tr key={row.date}>
                          <td><strong>{dayLabel(row.date)}</strong></td>
                          <td>{seconds(row.normalSeconds)}</td>
                          <td>
                            {row.uncertainSeconds > 0
                              ? (
                                <strong className="erp-uncertain-value">
                                  {seconds(row.uncertainSeconds)}
                                </strong>
                              )
                              : '-'}
                          </td>
                          <td>{row.issueCount}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </Panel>

              <Panel title="MATE별 실제 작업시간">
                <div className="erp-report-local-filter">
                  <label>
                    검색
                    <span>
                      <Search size={13}/>
                      <input
                        value={rangeKeyword}
                        onChange={(e) =>
                          setRangeKeyword(e.target.value)
                        }
                        placeholder="사원번호 / 별명"
                      />
                    </span>
                  </label>

                  <div>
                    조회 {filteredMateRows.length}
                    {' / '}
                    전체 {range.mates.length}
                  </div>
                </div>

                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>사원번호</th>
                        <th>MATE</th>
                        <th>Assignment</th>
                        <th>세션</th>
                        <th>정상 작업시간</th>
                        <th>UNCERTAIN 시간</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredMateRows.map((row) => (
                        <tr key={row.mateId}>
                          <td>{row.employeeNo}</td>
                          <td><strong>{row.nickname}</strong></td>
                          <td>{row.assignmentCount}</td>
                          <td>{row.sessionCount}</td>
                          <td>
                            <strong>
                              {seconds(row.normalSeconds)}
                            </strong>
                          </td>
                          <td>
                            {row.uncertainSeconds
                              ? seconds(row.uncertainSeconds)
                              : '-'}
                          </td>
                        </tr>
                      ))}

                      {filteredMateRows.length === 0 && (
                        <tr>
                          <td colSpan={6} className="empty-cell">
                            조건에 맞는 MATE 작업기록이 없습니다.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </Panel>

              <Panel title="구역 / 업무별 실제 작업시간">
                <div className="erp-report-local-filter erp-area-work-filter">
                  <label>
                    구역
                    <select
                      value={rangeArea}
                      onChange={(e) =>
                        setRangeArea(e.target.value)
                      }
                    >
                      <option value="ALL">전체</option>
                      {rangeAreas.map((area) => (
                        <option key={area}>{area}</option>
                      ))}
                    </select>
                  </label>

                  <label>
                    업무
                    <select
                      value={rangeWorkType}
                      onChange={(e) =>
                        setRangeWorkType(e.target.value)
                      }
                    >
                      <option value="ALL">전체</option>
                      {rangeWorkTypes.map((workType) => (
                        <option key={workType}>{workType}</option>
                      ))}
                    </select>
                  </label>

                  <div>
                    조회 {filteredAreaRows.length}
                    {' / '}
                    전체 {range.areaWorks.length}
                  </div>
                </div>

                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>구역</th>
                        <th>업무</th>
                        <th>Assignment</th>
                        <th>세션</th>
                        <th>정상 작업시간</th>
                        <th>UNCERTAIN 시간</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredAreaRows.map((row) => (
                        <tr
                          key={`${row.areaId}-${row.workTypeId}`}
                        >
                          <td><strong>{row.area}</strong></td>
                          <td>{row.workType}</td>
                          <td>{row.assignmentCount}</td>
                          <td>{row.sessionCount}</td>
                          <td>
                            <strong>
                              {seconds(row.normalSeconds)}
                            </strong>
                          </td>
                          <td>
                            {row.uncertainSeconds
                              ? seconds(row.uncertainSeconds)
                              : '-'}
                          </td>
                        </tr>
                      ))}

                      {filteredAreaRows.length === 0 && (
                        <tr>
                          <td colSpan={6} className="empty-cell">
                            조건에 맞는 구역/업무 기록이 없습니다.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </Panel>
            </>
          )}
        </>
      )}
    </div>
  )
}
