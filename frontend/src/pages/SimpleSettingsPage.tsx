import { useEffect, useMemo, useState } from 'react'
import { Save, Trash2 } from 'lucide-react'
import { useSearchParams } from 'react-router-dom'
import { api } from '../api/client'
import type { IssueType, Location, Mate, Pda, WorkType } from '../api/types'
import { Panel } from '../components/Panel'

const DAYS = [
  ['MONDAY', '월'],
  ['TUESDAY', '화'],
  ['WEDNESDAY', '수'],
  ['THURSDAY', '목'],
  ['FRIDAY', '금'],
  ['SATURDAY', '토'],
  ['SUNDAY', '일']
] as const

type DayKey = typeof DAYS[number][0]
type Tab = 'mate' | 'location' | 'pda' | 'work' | 'issue'
type ShiftDraft = Record<DayKey, {
  enabled: boolean
  shiftType: 'DAY' | 'CLOSING'
  startTime: string
  endTime: string
}>

function emptySchedule(): ShiftDraft {
  return Object.fromEntries(
    DAYS.map(([day]) => [day, {
      enabled: false,
      shiftType: 'DAY',
      startTime: '09:00',
      endTime: '18:00'
    }])
  ) as ShiftDraft
}

export function SimpleSettingsPage() {
  const [searchParams] = useSearchParams()
  const [tab, setTab] = useState<Tab>('mate')
  const [mates, setMates] = useState<Mate[]>([])
  const [pdas, setPdas] = useState<Pda[]>([])
  const [locations, setLocations] = useState<Location[]>([])
  const [workTypes, setWorkTypes] = useState<WorkType[]>([])
  const [issueTypes, setIssueTypes] = useState<IssueType[]>([])
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const load = async () => {
    setError('')
    try {
      const [m, p, l, w, i] = await Promise.all([
        api.mates(), api.pdas(), api.locations(), api.workTypes(), api.issueTypes()
      ])
      setMates(m)
      setPdas(p)
      setLocations(l)
      setWorkTypes(w)
      setIssueTypes(i)
    } catch (e) {
      setError(e instanceof Error ? e.message : '설정 정보를 불러오지 못했습니다.')
    }
  }

  useEffect(() => { void load() }, [])
  useEffect(() => {
    const requested = searchParams.get('tab')
    if (requested === 'mate' || requested === 'location' || requested === 'pda' || requested === 'work' || requested === 'issue') {
      setTab(requested)
    }
  }, [searchParams])

  const notify = (text: string) => {
    setMessage(text)
    window.setTimeout(() => setMessage(''), 2200)
  }

  return (
    <div className="settings-page">
      <div className="page-title-row">
        <div>
          <span className="eyebrow">SETTINGS</span>
          <h2>기본정보 설정</h2>
          <p>MATE, 로케이션, PDA, 업무 종류와 특이사항 구분만 관리합니다.</p>
        </div>
      </div>

      <div className="settings-tabs">
        {([
          ['mate', 'MATE · 근무스케줄'],
          ['location', '로케이션'],
          ['pda', 'PDA'],
          ['work', '업무 종류'],
          ['issue', '특이사항 구분']
        ] as [Tab, string][]).map(([key, label]) => (
          <button key={key} className={tab === key ? 'active' : ''} onClick={() => setTab(key)}>{label}</button>
        ))}
      </div>

      {message && <div className="toast-inline">{message}</div>}
      {error && <div className="error-state"><span>{error}</span></div>}

      {tab === 'mate' && <MateTab mates={mates} reload={load} notify={notify}/>} 
      {tab === 'location' && <LocationTab items={locations} reload={load} notify={notify}/>} 
      {tab === 'pda' && <PdaTab items={pdas} reload={load} notify={notify}/>} 
      {tab === 'work' && <WorkTab items={workTypes} reload={load} notify={notify}/>} 
      {tab === 'issue' && <IssueTypeTab items={issueTypes} reload={load} notify={notify}/>} 
    </div>
  )
}

function MateTab({ mates, reload, notify }: {
  mates: Mate[]
  reload: () => Promise<void>
  notify: (text: string) => void
}) {
  const [name, setName] = useState('')
  const [nickname, setNickname] = useState('')
  const [password, setPassword] = useState('1234')
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [nicknameDraft, setNicknameDraft] = useState('')
  const [schedule, setSchedule] = useState<ShiftDraft>(emptySchedule())

  const current = useMemo(
    () => mates.find((mate) => mate.id === selectedId) ?? null,
    [mates, selectedId]
  )

  useEffect(() => {
    if (!selectedId && mates[0]) setSelectedId(mates[0].id)
  }, [mates, selectedId])

  useEffect(() => {
    if (!current) return
    setNicknameDraft(current.nickname)
    void api.schedules(current.id).then((items) => {
      const next = emptySchedule()
      for (const item of items) {
        const key = item.dayOfWeek as DayKey
        if (!next[key]) continue
        next[key] = {
          enabled: true,
          shiftType: item.shiftType,
          startTime: item.startTime.slice(0, 5),
          endTime: item.endTime.slice(0, 5)
        }
      }
      setSchedule(next)
    })
  }, [current?.id])

  const createMate = async (e: React.FormEvent) => {
    e.preventDefault()
    const autoNickname = nickname.trim() || (name.trim().length > 1 ? name.trim().slice(1) : name.trim())
    await api.createMate({ name: name.trim(), nickname: autoNickname, password })
    setName('')
    setNickname('')
    await reload()
    notify('MATE를 등록했습니다.')
  }

  const saveNickname = async () => {
    if (!current || !nicknameDraft.trim()) return
    await api.updateNickname(current.id, nicknameDraft.trim())
    await reload()
    notify('별명을 수정했습니다.')
  }

  const toggleMate = async (mate: Mate) => {
    if (mate.active) {
      if (!confirm(`${mate.nickname} MATE를 삭제 처리할까요?`)) return
      await api.deactivateMate(mate.id)
      notify('MATE를 삭제 처리했습니다.')
    } else {
      await api.reactivateMate(mate.id)
      notify('MATE를 다시 사용하도록 변경했습니다.')
    }
    await reload()
  }

  const saveSchedule = async () => {
    if (!current) return
    const invalidDay = DAYS.find(([day]) => schedule[day].enabled && schedule[day].startTime === schedule[day].endTime)
    if (invalidDay) {
      notify(`${invalidDay[1]}요일 시작시간과 종료시간은 같을 수 없습니다.`)
      return
    }
    const payload = DAYS.flatMap(([day]) => {
      const item = schedule[day]
      if (!item.enabled) return []
      return [{
        dayOfWeek: day,
        scheduleType: day === 'SATURDAY' || day === 'SUNDAY' ? 'WEEKEND' as const : 'WEEKDAY' as const,
        shiftType: item.shiftType,
        startTime: `${item.startTime}:00`,
        endTime: `${item.endTime}:00`
      }]
    })
    await api.replaceSchedules(current.id, payload)
    notify('근무스케줄을 저장했습니다.')
  }

  return (
    <div className="stack-page">
      <Panel title="MATE 등록">
        <form className="compact-form" onSubmit={createMate}>
          <div className="form-grid-2">
            <label>이름<input value={name} onChange={(e) => setName(e.target.value)} required/></label>
            <label>별명<input value={nickname} onChange={(e) => setNickname(e.target.value)} placeholder="비우면 이름에서 성 제외"/></label>
            <label>초기 비밀번호<input value={password} onChange={(e) => setPassword(e.target.value)} required/></label>
          </div>
          <button className="primary-button">등록</button>
        </form>
      </Panel>

      <Panel title="MATE 목록">
        <div className="table-wrap">
          <table>
            <thead><tr><th>사원번호</th><th>이름</th><th>별명</th><th>상태</th><th>사용여부</th><th>선택</th><th>삭제</th></tr></thead>
            <tbody>
              {mates.map((mate) => (
                <tr key={mate.id} className={selectedId === mate.id ? 'selected-row' : ''}>
                  <td>{mate.employeeNo}</td>
                  <td>{mate.name}</td>
                  <td><strong>{mate.nickname}</strong></td>
                  <td>{mate.status}</td>
                  <td>{mate.active ? '사용' : '삭제됨'}</td>
                  <td><button className="erp-row-button" onClick={() => setSelectedId(mate.id)}>스케줄/수정</button></td>
                  <td><button className="danger-text-button" onClick={() => void toggleMate(mate)}>{mate.active ? '삭제' : '복구'}</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Panel>

      {current && (
        <Panel title={`${current.nickname} · 근무스케줄`}>
          <div className="compact-form">
            <label>
              별명 수정
              <div className="inline-edit">
                <input value={nicknameDraft} onChange={(e) => setNicknameDraft(e.target.value)}/>
                <button type="button" onClick={() => void saveNickname()}><Save size={13}/></button>
              </div>
            </label>
          </div>

          <div className="schedule-grid erp-schedule-grid">
            <div className="schedule-header-row"><span>사용</span><span>요일</span><span>주간/마감</span><span>시작</span><span></span><span>종료</span></div>
            {DAYS.map(([day, label]) => {
              const item = schedule[day]
              return (
                <div className={`schedule-row ${item.enabled ? 'enabled' : ''}`} key={day}>
                  <label className="day-check"><input type="checkbox" checked={item.enabled} onChange={(e) => setSchedule((prev) => ({ ...prev, [day]: { ...item, enabled: e.target.checked } }))}/></label>
                  <strong>{label}</strong>
                  <select disabled={!item.enabled} value={item.shiftType} onChange={(e) => setSchedule((prev) => ({ ...prev, [day]: { ...item, shiftType: e.target.value as 'DAY' | 'CLOSING' } }))}>
                    <option value="DAY">주간</option>
                    <option value="CLOSING">마감</option>
                  </select>
                  <input disabled={!item.enabled} type="time" value={item.startTime} onChange={(e) => setSchedule((prev) => ({ ...prev, [day]: { ...item, startTime: e.target.value } }))}/>
                  <span>~</span>
                  <input disabled={!item.enabled} type="time" value={item.endTime} onChange={(e) => setSchedule((prev) => ({ ...prev, [day]: { ...item, endTime: e.target.value } }))}/>
                </div>
              )
            })}
          </div>
          <button className="primary-button compact" onClick={() => void saveSchedule()}><Save size={13}/> 스케줄 저장</button>
        </Panel>
      )}
    </div>
  )
}

function LocationTab({ items, reload, notify }: {
  items: Location[]
  reload: () => Promise<void>
  notify: (text: string) => void
}) {
  const [alphabet, setAlphabet] = useState('A')
  const [start, setStart] = useState('1')
  const [end, setEnd] = useState('3')
  const [floor, setFloor] = useState('1')
  const [foodType, setFoodType] = useState<'NON_FOOD' | 'FOOD'>('NON_FOOD')
  const [categories, setCategories] = useState<string[]>(['GENERAL'])
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [editFloor, setEditFloor] = useState('')
  const [editFood, setEditFood] = useState<'NON_FOOD' | 'FOOD'>('NON_FOOD')
  const [editCategories, setEditCategories] = useState<string[]>([])

  const current = items.find((item) => item.id === selectedId) ?? null

  useEffect(() => {
    if (!current) return
    setEditFloor(current.floor == null ? '' : String(current.floor))
    setEditFood(current.foodType)
    setEditCategories(current.nonFoodCategories)
  }, [current?.id])

  const toggle = (value: string, edit = false) => {
    const setter = edit ? setEditCategories : setCategories
    setter((prev) => prev.includes(value) ? prev.filter((x) => x !== value) : [...prev, value])
  }

  const create = async (e: React.FormEvent) => {
    e.preventDefault()
    await api.createAreaRange({
      alphabet: alphabet.trim().toUpperCase(),
      startNumber: Number(start),
      endNumber: Number(end),
      width: 2,
      floor: Number(floor),
      foodType,
      nonFoodCategories: foodType === 'NON_FOOD' ? categories : []
    })
    await reload()
    notify('로케이션을 등록했습니다.')
  }

  const save = async () => {
    if (!current) return
    await api.updateLocationMetadata(current.id, {
      floor: editFloor === '' ? null : Number(editFloor),
      foodType: editFood,
      nonFoodCategories: editFood === 'NON_FOOD' ? editCategories : []
    })
    await reload()
    notify('로케이션 정보를 수정했습니다.')
  }

  const remove = async (item: Location) => {
    if (!confirm(`${item.fullCode} 로케이션을 삭제 처리할까요?`)) return
    await api.deactivateLocation(item.id)
    await reload()
    notify('로케이션을 삭제 처리했습니다.')
  }

  return (
    <div className="stack-page">
      <Panel title="로케이션 등록">
        <form className="compact-form" onSubmit={create}>
          <div className="form-grid-2">
            <label>알파벳<input value={alphabet} onChange={(e) => setAlphabet(e.target.value.toUpperCase())} required/></label>
            <label>층수<input type="number" value={floor} onChange={(e) => setFloor(e.target.value)} required/></label>
            <label>시작점<input type="number" value={start} onChange={(e) => setStart(e.target.value)} required/></label>
            <label>종료점<input type="number" value={end} onChange={(e) => setEnd(e.target.value)} required/></label>
          </div>
          <div className="erp-form-row">
            <label><input type="radio" checked={foodType === 'NON_FOOD'} onChange={() => setFoodType('NON_FOOD')}/> 비식품</label>
            <label><input type="radio" checked={foodType === 'FOOD'} onChange={() => setFoodType('FOOD')}/> 식품</label>
          </div>
          {foodType === 'NON_FOOD' && <CategoryChecks values={categories} onToggle={(value) => toggle(value)}/>} 
          <button className="primary-button">등록</button>
        </form>
      </Panel>

      <Panel title="로케이션 목록">
        <div className="table-wrap">
          <table>
            <thead><tr><th>로케이션</th><th>층</th><th>구분</th><th>비식품 분류</th><th>상태</th><th>수정</th><th>삭제</th></tr></thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.id} className={selectedId === item.id ? 'selected-row' : ''}>
                  <td><strong>{item.fullCode}</strong></td>
                  <td>{item.floor ?? '-'}</td>
                  <td>{item.foodType === 'FOOD' ? '식품' : '비식품'}</td>
                  <td>{item.nonFoodCategories.join(', ') || '-'}</td>
                  <td>{item.active ? '사용' : '삭제됨'}</td>
                  <td><button className="erp-row-button" onClick={() => setSelectedId(item.id)}>수정</button></td>
                  <td>{item.active && <button className="danger-text-button" onClick={() => void remove(item)}>삭제</button>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Panel>

      {current && (
        <Panel title={`${current.fullCode} 수정`}>
          <div className="compact-form">
            <div className="form-grid-2">
              <label>층수<input type="number" value={editFloor} onChange={(e) => setEditFloor(e.target.value)}/></label>
              <label>구분<select value={editFood} onChange={(e) => setEditFood(e.target.value as 'NON_FOOD' | 'FOOD')}><option value="NON_FOOD">비식품</option><option value="FOOD">식품</option></select></label>
            </div>
            {editFood === 'NON_FOOD' && <CategoryChecks values={editCategories} onToggle={(value) => toggle(value, true)}/>} 
            <button className="primary-button compact" onClick={() => void save()}><Save size={13}/> 수정 저장</button>
          </div>
        </Panel>
      )}
    </div>
  )
}

function CategoryChecks({ values, onToggle }: { values: string[]; onToggle: (value: string) => void }) {
  const categories = [
    ['GENERAL', '일반'],
    ['COLOR', '색조'],
    ['HYGIENE', '위생'],
    ['TOOLS', '도구']
  ] as const
  return (
    <div className="erp-checkbox-grid">
      {categories.map(([value, label]) => (
        <label key={value}><input type="checkbox" checked={values.includes(value)} onChange={() => onToggle(value)}/>{label}</label>
      ))}
    </div>
  )
}

function PdaTab({ items, reload, notify }: {
  items: Pda[]
  reload: () => Promise<void>
  notify: (text: string) => void
}) {
  const [number, setNumber] = useState('')
  const [edit, setEdit] = useState<Record<number, string>>({})

  const create = async (e: React.FormEvent) => {
    e.preventDefault()
    await api.createPda(Number(number))
    setNumber('')
    await reload()
    notify('PDA를 등록했습니다.')
  }

  const save = async (item: Pda) => {
    const next = Number(edit[item.id] || item.deviceNumber)
    await api.updatePdaNumber(item.id, next)
    await reload()
    notify('PDA 번호를 수정했습니다.')
  }

  const remove = async (item: Pda) => {
    if (!confirm(`PDA ${item.deviceNumber}을 삭제할까요?`)) return
    await api.deletePda(item.id)
    await reload()
    notify('PDA를 삭제/폐기 처리했습니다.')
  }

  return (
    <div className="stack-page">
      <Panel title="PDA 등록">
        <form className="compact-form" onSubmit={create}>
          <label>PDA 번호<input type="number" min="1" value={number} onChange={(e) => setNumber(e.target.value)} required/></label>
          <button className="primary-button">등록</button>
        </form>
      </Panel>
      <Panel title="PDA 목록">
        <div className="table-wrap">
          <table>
            <thead><tr><th>번호</th><th>상태</th><th>번호 수정</th><th>삭제</th></tr></thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.id}>
                  <td><strong>{item.deviceNumber}</strong></td>
                  <td>{item.status}</td>
                  <td><div className="inline-edit"><input type="number" value={edit[item.id] ?? ''} placeholder={String(item.deviceNumber)} onChange={(e) => setEdit((prev) => ({ ...prev, [item.id]: e.target.value }))}/><button onClick={() => void save(item)}><Save size={13}/></button></div></td>
                  <td><button className="danger-text-button" onClick={() => void remove(item)}><Trash2 size={13}/> 삭제</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Panel>
    </div>
  )
}

function WorkTab({ items, reload, notify }: {
  items: WorkType[]
  reload: () => Promise<void>
  notify: (text: string) => void
}) {
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const current = items.find((item) => item.id === selectedId) ?? null
  const [editName, setEditName] = useState('')
  const [editDescription, setEditDescription] = useState('')

  useEffect(() => {
    if (!current) return
    setEditName(current.name)
    setEditDescription(current.description ?? '')
  }, [current?.id])

  const create = async (e: React.FormEvent) => {
    e.preventDefault()
    await api.createWorkType({ name, description })
    setName('')
    setDescription('')
    await reload()
    notify('업무 종류를 등록했습니다.')
  }

  const save = async () => {
    if (!current) return
    await api.updateWorkType(current.id, { name: editName, description: editDescription })
    await reload()
    notify('업무 종류를 수정했습니다.')
  }

  const remove = async (item: WorkType) => {
    if (!confirm(`${item.name} 업무 종류를 삭제 처리할까요?`)) return
    await api.deactivateWorkType(item.id)
    await reload()
    notify('업무 종류를 삭제 처리했습니다.')
  }

  return (
    <div className="stack-page">
      <Panel title="업무 종류 등록">
        <form className="compact-form" onSubmit={create}><label>업무명<input value={name} onChange={(e) => setName(e.target.value)} required/></label><label>업무 설명<textarea value={description} onChange={(e) => setDescription(e.target.value)}/></label><button className="primary-button">등록</button></form>
      </Panel>
      <Panel title="업무 종류 목록">
        <div className="table-wrap"><table><thead><tr><th>업무명</th><th>설명</th><th>상태</th><th>수정</th><th>삭제</th></tr></thead><tbody>{items.map((item) => <tr key={item.id}><td><strong>{item.name}</strong></td><td>{item.description ?? '-'}</td><td>{item.active ? '사용' : '삭제됨'}</td><td><button className="erp-row-button" onClick={() => setSelectedId(item.id)}>수정</button></td><td>{item.active && <button className="danger-text-button" onClick={() => void remove(item)}>삭제</button>}</td></tr>)}</tbody></table></div>
      </Panel>
      {current && <Panel title={`${current.name} 수정`}><div className="compact-form"><label>업무명<input value={editName} onChange={(e) => setEditName(e.target.value)}/></label><label>업무 설명<textarea value={editDescription} onChange={(e) => setEditDescription(e.target.value)}/></label><button className="primary-button compact" onClick={() => void save()}><Save size={13}/> 수정 저장</button></div></Panel>}
    </div>
  )
}

function IssueTypeTab({ items, reload, notify }: {
  items: IssueType[]
  reload: () => Promise<void>
  notify: (text: string) => void
}) {
  const blank = { name: '', requireLocation: true, requireProductCode: false, requireQuantity: false }
  const [form, setForm] = useState(blank)
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const current = items.find((item) => item.id === selectedId) ?? null
  const [edit, setEdit] = useState(blank)

  useEffect(() => {
    if (!current) return
    setEdit({
      name: current.name,
      requireLocation: current.requireLocation,
      requireProductCode: current.requireProductCode,
      requireQuantity: current.requireQuantity
    })
  }, [current?.id])

  const create = async (e: React.FormEvent) => {
    e.preventDefault()
    await api.createIssueType(form)
    setForm(blank)
    await reload()
    notify('특이사항 구분을 등록했습니다.')
  }

  const save = async () => {
    if (!current) return
    await api.updateIssueType(current.id, edit)
    await reload()
    notify('특이사항 구분을 수정했습니다.')
  }

  const remove = async (item: IssueType) => {
    if (!confirm(`${item.name} 구분을 삭제 처리할까요?`)) return
    await api.deactivateIssueType(item.id)
    await reload()
    notify('특이사항 구분을 삭제 처리했습니다.')
  }

  return (
    <div className="stack-page">
      <Panel title="특이사항 구분 등록">
        <form className="compact-form" onSubmit={create}>
          <label>구분명<input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required/></label>
          <RequirementChecks value={form} setValue={setForm}/>
          <small>코멘트 입력은 기본으로 항상 포함됩니다. 수량 선택 시 MATE 화면에서 재고 관련 입력란이 함께 표시됩니다.</small>
          <button className="primary-button">등록</button>
        </form>
      </Panel>
      <Panel title="특이사항 구분 목록">
        <div className="table-wrap"><table><thead><tr><th>구분</th><th>로케이션</th><th>상품코드</th><th>수량</th><th>상태</th><th>수정</th><th>삭제</th></tr></thead><tbody>{items.map((item) => <tr key={item.id}><td><strong>{item.name}</strong></td><td>{item.requireLocation ? '사용' : '-'}</td><td>{item.requireProductCode ? '사용' : '-'}</td><td>{item.requireQuantity ? '사용' : '-'}</td><td>{item.active ? '사용' : '삭제됨'}</td><td><button className="erp-row-button" onClick={() => setSelectedId(item.id)}>수정</button></td><td>{item.active && <button className="danger-text-button" onClick={() => void remove(item)}>삭제</button>}</td></tr>)}</tbody></table></div>
      </Panel>
      {current && <Panel title={`${current.name} 수정`}><div className="compact-form"><label>구분명<input value={edit.name} onChange={(e) => setEdit({ ...edit, name: e.target.value })}/></label><RequirementChecks value={edit} setValue={setEdit}/><button className="primary-button compact" onClick={() => void save()}><Save size={13}/> 수정 저장</button></div></Panel>}
    </div>
  )
}

function RequirementChecks({ value, setValue }: {
  value: { name: string; requireLocation: boolean; requireProductCode: boolean; requireQuantity: boolean }
  setValue: (value: { name: string; requireLocation: boolean; requireProductCode: boolean; requireQuantity: boolean }) => void
}) {
  return (
    <div className="erp-checkbox-grid">
      <label><input type="checkbox" checked={value.requireLocation} onChange={(e) => setValue({ ...value, requireLocation: e.target.checked })}/>로케이션</label>
      <label><input type="checkbox" checked={value.requireProductCode} onChange={(e) => setValue({ ...value, requireProductCode: e.target.checked })}/>상품코드</label>
      <label><input type="checkbox" checked={value.requireQuantity} onChange={(e) => setValue({ ...value, requireQuantity: e.target.checked })}/>수량</label>
      <label><input type="checkbox" checked readOnly/>코멘트(기본)</label>
    </div>
  )
}
