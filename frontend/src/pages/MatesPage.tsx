import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { CalendarClock, History, Pencil, Power, RefreshCw, Save, Search } from 'lucide-react'
import { api } from '../api/client'
import type { Mate, ScheduleItem } from '../api/types'
import { Panel } from '../components/Panel'
import { StatusBadge } from '../components/StatusBadge'

const days = [
  ['MONDAY','월'], ['TUESDAY','화'], ['WEDNESDAY','수'], ['THURSDAY','목'],
  ['FRIDAY','금'], ['SATURDAY','토'], ['SUNDAY','일']
] as const

export function MatesPage() {
  const [searchParams] = useSearchParams()
  const [mates, setMates] = useState<Mate[]>([])
  const [selected, setSelected] = useState<number | null>(null)
  const [scheduleDraft, setScheduleDraft] = useState<
    Record<string, {
      enabled:boolean
      shiftType:'DAY'|'CLOSING'
      startTime:string
      endTime:string
    }>
  >({})
  const [override, setOverride] = useState({
    startDate:'',
    endDate:'',
    startTime:'09:00',
    endTime:'18:00'
  })
  const [message, setMessage] = useState('')
  const [keyword, setKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [nicknameEdit, setNicknameEdit] = useState(false)
  const [nicknameDraft, setNicknameDraft] = useState('')

  const loadMates = async () => {
    const data = await api.mates()
    setMates(data)

    const requestedMateId = searchParams.get('mateId')

    if (requestedMateId) {
      const requested = Number(requestedMateId)
      if (data.some((mate) => mate.id === requested)) {
        setSelected(requested)
        return
      }
    }

    if (!selected && data[0]) {
      setSelected(data[0].id)
    }
  }

  useEffect(() => {
    void loadMates()
  }, [])

  useEffect(() => {
    if (!selected) return

    void api.schedules(selected).then((items) => {
      const draft: typeof scheduleDraft = {}

      for (const [day] of days) {
        const found = items.find((item) => item.dayOfWeek === day)

        draft[day] = found
          ? {
              enabled:true,
              shiftType:found.shiftType,
              startTime:found.startTime.slice(0,5),
              endTime:found.endTime.slice(0,5)
            }
          : {
              enabled:false,
              shiftType:'DAY',
              startTime:'09:00',
              endTime:'18:00'
            }
      }

      setScheduleDraft(draft)
    })
  }, [selected])

  const current = useMemo(
    () => mates.find((mate) => mate.id === selected) ?? null,
    [mates, selected]
  )

  useEffect(() => {
    if (!current) return
    setNicknameDraft(current.nickname)
    setNicknameEdit(false)
  }, [current?.id])

  const filteredMates = useMemo(() => {
    const q = keyword.trim().toUpperCase()

    return mates.filter((mate) => {
      if (statusFilter !== 'ALL' && mate.status !== statusFilter) return false
      if (!q) return true

      return [
        mate.employeeNo,
        mate.name,
        mate.nickname,
        mate.whereabouts ?? ''
      ].join(' ').toUpperCase().includes(q)
    })
  }, [mates, keyword, statusFilter])

  const saveSchedule = async () => {
    if (!selected) return

    const payload = days.flatMap(([day]) => {
      const draft = scheduleDraft[day]
      if (!draft?.enabled) return []

      return [{
        dayOfWeek: day,
        scheduleType:
          day === 'SATURDAY' || day === 'SUNDAY'
            ? 'WEEKEND' as const
            : 'WEEKDAY' as const,
        shiftType: draft.shiftType,
        startTime: `${draft.startTime}:00`,
        endTime: `${draft.endTime}:00`
      }]
    })

    if (
      payload.some(
        (item) => item.startTime === item.endTime
      )
    ) {
      setMessage(
        '근무 시작시간과 종료시간은 같을 수 없습니다.'
      )
      return
    }

    try {
      await api.replaceSchedules(selected, payload)
      setMessage('기본 근무스케줄을 저장했습니다.')
    } catch (e) {
      setMessage(
        e instanceof Error
          ? e.message
          : '기본 근무스케줄을 저장하지 못했습니다.'
      )
    }
  }

  const saveOverride = async () => {
    if (!selected) return

    if (override.startTime === override.endTime) {
      setMessage(
        '예외 근무의 시작시간과 종료시간은 같을 수 없습니다.'
      )
      return
    }

    try {
      await api.createScheduleOverride(selected, override)
      setMessage('기간별 예외 근무시간을 저장했습니다.')
    } catch (e) {
      setMessage(
        e instanceof Error
          ? e.message
          : '기간별 예외 근무시간을 저장하지 못했습니다.'
      )
    }
  }

  const saveNickname = async () => {
    if (!current || !nicknameDraft.trim()) return

    await api.updateNickname(current.id, nicknameDraft.trim())
    await loadMates()
    setNicknameEdit(false)
    setMessage('운영 별명을 수정했습니다.')
  }

  const toggleActive = async () => {
    if (!current) return

    if (current.active) {
      if (!confirm(`${current.nickname} MATE를 비활성 처리할까요?`)) return
      await api.deactivateMate(current.id)
    } else {
      await api.reactivateMate(current.id)
    }

    await loadMates()
    setMessage(
      current.active
        ? 'MATE를 비활성 처리했습니다.'
        : 'MATE를 재활성했습니다.'
    )
  }

  return (
    <div className="stack-page">
      <div className="page-title-row">
        <div>
          <span className="eyebrow">MATE MANAGEMENT</span>
          <h2>MATE 현황</h2>
          <p>근무상태·기본 스케줄·예외 근무시간과 운영 별명을 관리합니다.</p>
        </div>
      </div>

      {message && <div className="toast-inline">{message}</div>}

      <div className="mate-management-grid erp-mate-management-grid">
        <Panel title="MATE 조회">
          <div className="erp-mate-filter">
            <label>
              검색
              <span>
                <Search size={13}/>
                <input
                  value={keyword}
                  onChange={(e)=>setKeyword(e.target.value)}
                  placeholder="사번 / 이름 / 별명"
                />
              </span>
            </label>

            <label>
              상태
              <select
                value={statusFilter}
                onChange={(e)=>setStatusFilter(e.target.value)}
              >
                <option value="ALL">전체</option>
                <option value="AVAILABLE">대기</option>
                <option value="WORKING">업무중</option>
                <option value="BREAK">휴게</option>
                <option value="AWAY">자리비움</option>
                <option value="OFF_DUTY">퇴근</option>
              </select>
            </label>
          </div>

          <div className="table-wrap mate-master-table">
            <table>
              <thead>
                <tr>
                  <th>사원번호</th>
                  <th>별명</th>
                  <th>실명</th>
                  <th>상태</th>
                  <th>거소</th>
                  <th>활성</th>
                </tr>
              </thead>
              <tbody>
                {filteredMates.map((mate)=>(
                  <tr
                    key={mate.id}
                    className={selected===mate.id?'selected-row clickable-row':'clickable-row'}
                    onClick={()=>setSelected(mate.id)}
                  >
                    <td>{mate.employeeNo}</td>
                    <td><strong>{mate.nickname}</strong></td>
                    <td>{mate.name}</td>
                    <td><StatusBadge status={mate.status}/></td>
                    <td>{mate.whereabouts??'-'}</td>
                    <td>{mate.active?'Y':'N'}</td>
                  </tr>
                ))}
                {filteredMates.length===0&&(
                  <tr><td colSpan={6} className="empty-cell">조건에 맞는 MATE가 없습니다.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </Panel>

        <div className="mate-detail-column">
          {current && (
            <>
              <Panel
                title={`기본 정보 · ${current.employeeNo}`}
                action={
                  <div className="erp-cell-actions">
                    <Link
                      className="erp-row-button"
                      to={`/audit-logs?referenceType=MATE&referenceId=${current.id}`}
                    >
                      <History size={12}/>
                      기본정보 이력
                    </Link>
                    <button
                      className="secondary-button compact"
                      onClick={()=>void toggleActive()}
                    >
                      <Power size={14}/>
                      {current.active?'비활성':'재활성'}
                    </button>
                  </div>
                }
              >
                <div className="erp-profile-grid">
                  <div><span>실명</span><strong>{current.name}</strong></div>
                  <div>
                    <span>운영 별명</span>
                    {nicknameEdit?(
                      <div className="inline-edit">
                        <input
                          value={nicknameDraft}
                          onChange={(e)=>setNicknameDraft(e.target.value)}
                        />
                        <button onClick={()=>void saveNickname()}><Save size={13}/></button>
                      </div>
                    ):(
                      <strong>
                        {current.nickname}
                        <button className="erp-inline-icon" onClick={()=>setNicknameEdit(true)}>
                          <Pencil size={12}/>
                        </button>
                      </strong>
                    )}
                  </div>
                  <div><span>상태</span><strong><StatusBadge status={current.status}/></strong></div>
                  <div><span>거소</span><strong>{current.whereabouts??'-'}</strong></div>
                  <div><span>입사일</span><strong>{current.joinedAt}</strong></div>
                  <div><span>활성</span><strong>{current.active?'ACTIVE':'INACTIVE'}</strong></div>
                </div>
              </Panel>

              <Panel
                title="기본 근무스케줄"
                action={
                  <div className="erp-cell-actions">
                    <Link
                      className="erp-row-button"
                      to={`/audit-logs?referenceType=MATE_SCHEDULE&referenceId=${current.id}`}
                    >
                      <History size={12}/>
                      변경이력
                    </Link>
                    <button className="primary-button compact" onClick={()=>void saveSchedule()}>
                      <Save size={13}/> 저장
                    </button>
                  </div>
                }
              >
                <div className="schedule-grid erp-schedule-grid">
                  <div className="schedule-header-row">
                    <span>사용</span><span>요일</span><span>구분</span>
                    <span>시작</span><span></span><span>종료</span>
                  </div>

                  {days.map(([day,label]) => {
                    const draft = scheduleDraft[day] ?? {
                      enabled:false,
                      shiftType:'DAY' as const,
                      startTime:'09:00',
                      endTime:'18:00'
                    }

                    return (
                      <div
                        className={`schedule-row ${draft.enabled?'enabled':''}`}
                        key={day}
                      >
                        <label className="day-check">
                          <input
                            type="checkbox"
                            checked={draft.enabled}
                            onChange={(e)=>
                              setScheduleDraft((prev)=>({
                                ...prev,
                                [day]:{...draft,enabled:e.target.checked}
                              }))
                            }
                          />
                        </label>
                        <strong>{label}</strong>
                        <select
                          disabled={!draft.enabled}
                          value={draft.shiftType}
                          onChange={(e)=>
                            setScheduleDraft((prev)=>({
                              ...prev,
                              [day]:{
                                ...draft,
                                shiftType:e.target.value as 'DAY'|'CLOSING'
                              }
                            }))
                          }
                        >
                          <option value="DAY">주간</option>
                          <option value="CLOSING">마감</option>
                        </select>
                        <input
                          disabled={!draft.enabled}
                          type="time"
                          value={draft.startTime}
                          onChange={(e)=>
                            setScheduleDraft((prev)=>({
                              ...prev,
                              [day]:{...draft,startTime:e.target.value}
                            }))
                          }
                        />
                        <span>~</span>
                        <input
                          disabled={!draft.enabled}
                          type="time"
                          value={draft.endTime}
                          onChange={(e)=>
                            setScheduleDraft((prev)=>({
                              ...prev,
                              [day]:{...draft,endTime:e.target.value}
                            }))
                          }
                        />
                      </div>
                    )
                  })}
                </div>
                <p className="hint-copy erp-overnight-help">
                  종료가 시작보다 빠르면 익일 종료로 처리합니다.
                  예: 22:00 ~ 06:00 = 다음 날 06:00 종료.
                  시작/종료가 같으면 저장되지 않습니다.
                </p>
              </Panel>

              <Panel title="기간별 예외 근무시간">
                <div className="override-form">
                  <label>
                    기준 시작일
                    <input
                      type="date"
                      value={override.startDate}
                      onChange={(e)=>setOverride({...override,startDate:e.target.value})}
                    />
                  </label>
                  <label>
                    기준 종료일
                    <input
                      type="date"
                      value={override.endDate}
                      onChange={(e)=>setOverride({...override,endDate:e.target.value})}
                    />
                  </label>
                  <label>
                    시작
                    <input
                      type="time"
                      value={override.startTime}
                      onChange={(e)=>setOverride({...override,startTime:e.target.value})}
                    />
                  </label>
                  <label>
                    종료
                    <input
                      type="time"
                      value={override.endTime}
                      onChange={(e)=>setOverride({...override,endTime:e.target.value})}
                    />
                  </label>
                  <button
                    className="primary-button"
                    onClick={()=>void saveOverride()}
                    disabled={!override.startDate||!override.endDate}
                  >
                    <CalendarClock size={14}/> 적용
                  </button>
                  <button
                    className="secondary-button"
                    onClick={()=>
                      selected&&void api.extendMateToday(selected).then(()=>
                        setMessage('오늘 연장 상태를 활성화했습니다.')
                      )
                    }
                  >
                    <RefreshCw size={14}/> 오늘 연장
                  </button>
                </div>

                <p className="hint-copy">
                  기간별 지정시간이 기본 스케줄보다 우선합니다. 종료가 시작보다 빠르면 익일 종료입니다. 연장 상태에서는 자동종료가 해제되고 MATE가 직접 근무종료합니다.
                </p>
              </Panel>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
