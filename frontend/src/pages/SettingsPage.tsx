import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ArrowRightLeft, History, Pencil, Plus, Save, Trash2 } from 'lucide-react'
import { api } from '../api/client'
import type { IssueType, Location, Mate, Pda, PdaUsage, WorkType } from '../api/types'
import { Panel } from '../components/Panel'
import { StatusBadge } from '../components/StatusBadge'

type Tab = 'mate' | 'location' | 'pda' | 'work' | 'issue'

export function SettingsPage() {
  const [searchParams] = useSearchParams()
  const [tab, setTab] = useState<Tab>('mate')
  const [mates, setMates] = useState<Mate[]>([])
  const [pdas, setPdas] = useState<Pda[]>([])
  const [locations, setLocations] = useState<Location[]>([])
  const [workTypes, setWorkTypes] = useState<WorkType[]>([])
  const [issueTypes, setIssueTypes] = useState<IssueType[]>([])
  const [message, setMessage] = useState('')

  const load = async () => {
    const [m,p,l,w,i] = await Promise.all([api.mates(),api.pdas(),api.locations(),api.workTypes(),api.issueTypes()])
    setMates(m); setPdas(p); setLocations(l); setWorkTypes(w); setIssueTypes(i)
  }
  useEffect(() => { void load() }, [])
  useEffect(() => {
    const requested = searchParams.get('tab')
    if (
      requested === 'mate'
        || requested === 'location'
        || requested === 'pda'
        || requested === 'work'
        || requested === 'issue'
    ) {
      setTab(requested)
    }
  }, [searchParams])

  const notify = (text:string) => {
    setMessage(text)
    window.setTimeout(() => setMessage(''),2200)
  }

  return (
    <div className="settings-page">
      <div className="page-title-row"><div><span className="eyebrow">ADMIN SETTINGS</span><h2>설정</h2><p>기준정보는 이력 참조가 있는 경우 삭제보다 비활성화를 우선합니다.</p></div></div>
      <div className="settings-tabs">
        {([['mate','MATE'],['location','로케이션'],['pda','PDA 기기'],['work','업무 종류'],['issue','특이사항']] as [Tab,string][]).map(([key,label]) => <button key={key} className={tab===key?'active':''} onClick={()=>setTab(key)}>{label}</button>)}
      </div>
      {message && <div className="toast-inline">{message}</div>}

      {tab==='mate' && <MateSettings mates={mates} reload={async()=>{await load();notify('MATE 정보를 갱신했습니다.')}}/>}
      {tab==='pda' && <PdaSettings pdas={pdas} reload={async()=>{await load();notify('PDA 정보를 갱신했습니다.')}}/>}
      {tab==='location' && <LocationSettings locations={locations} reload={async()=>{await load();notify('로케이션 정보를 갱신했습니다.')}}/>}
      {tab==='work' && <WorkTypeSettings items={workTypes} reload={async()=>{await load();notify('업무 종류를 갱신했습니다.')}}/>}
      {tab==='issue' && <IssueTypeSettings items={issueTypes} reload={async()=>{await load();notify('특이사항 구분을 갱신했습니다.')}}/>}
    </div>
  )
}

function MateSettings({mates,reload}:{mates:Mate[];reload:()=>Promise<void>}) {
  const [name,setName]=useState(''); const [nickname,setNickname]=useState(''); const [password,setPassword]=useState('1234')
  const submit=async(e:React.FormEvent)=>{e.preventDefault();await api.createMate({name,nickname,password});setName('');setNickname('');await reload()}
  return <Panel title="MATE 관리"><div className="split-admin">
    <form className="compact-form" onSubmit={submit}><h3>신규 MATE 등록</h3><label>이름<input value={name} onChange={(e)=>setName(e.target.value)} required/></label><label>별명<input value={nickname} onChange={(e)=>setNickname(e.target.value)} required/></label><label>초기 비밀번호<input value={password} onChange={(e)=>setPassword(e.target.value)} required/></label><button className="primary-button">등록</button><small>사원번호는 MT Prefix + 순번으로 자동발급되며 변경되지 않습니다.</small></form>
    <div className="table-wrap"><table><thead><tr><th>사원번호</th><th>이름</th><th>별명</th><th>상태</th><th>활성</th><th>감사</th></tr></thead><tbody>{mates.map((m)=><tr key={m.id}><td>{m.employeeNo}</td><td>{m.name}</td><td><strong>{m.nickname}</strong></td><td><StatusBadge status={m.status}/></td><td>{m.active?'Y':'N'}</td><td><Link className="erp-row-button" to={`/audit-logs?referenceType=MATE&referenceId=${m.id}`}><History size={12}/> 이력</Link></td></tr>)}</tbody></table></div>
  </div></Panel>
}

function PdaSettings({pdas,reload}:{pdas:Pda[];reload:()=>Promise<void>}) {
  const [num,setNum]=useState('')
  const [checked,setChecked]=useState<number[]>([])
  const [edit,setEdit]=useState<Record<number,string>>({})
  const [historyDevice,setHistoryDevice]=useState<Pda|null>(null)
  const [history,setHistory]=useState<PdaUsage[]>([])

  const submit=async(e:React.FormEvent)=>{
    e.preventDefault()
    await api.createPda(Number(num))
    setNum('')
    await reload()
  }

  const toggle=(id:number)=>
    setChecked((v)=>v.includes(id)?v.filter((x)=>x!==id):v.length<2?[...v,id]:[v[1],id])

  const swap=async()=>{
    if(checked.length!==2)return
    await api.swapPdaNumbers(checked[0],checked[1])
    setChecked([])
    await reload()
  }

  const changeNumber=async(pda:Pda)=>{
    const n=Number(edit[pda.id]??pda.deviceNumber)
    await api.updatePdaNumber(pda.id,n)
    setEdit((v)=>({...v,[pda.id]:''}))
    await reload()
  }

  const remove=async(pda:Pda)=>{
    if(!confirm(`PDA ${pda.deviceNumber}을 삭제/폐기 처리할까요?`))return
    await api.deletePda(pda.id)
    if(historyDevice?.id===pda.id){
      setHistoryDevice(null)
      setHistory([])
    }
    await reload()
  }

  const openHistory=async(pda:Pda)=>{
    if(historyDevice?.id===pda.id){
      setHistoryDevice(null)
      setHistory([])
      return
    }
    setHistoryDevice(pda)
    setHistory(await api.pdaUsageHistory(pda.id))
  }

  const fmt=(v:string|null)=>
    v?new Intl.DateTimeFormat('ko-KR',{
      year:'2-digit',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit'
    }).format(new Date(v)):'-'

  return (
    <Panel
      title="PDA 기기 관리"
      action={
        <button
          className="secondary-button compact"
          disabled={checked.length!==2}
          onClick={()=>void swap()}
        >
          <ArrowRightLeft size={13}/> 선택 2대 번호 교체
        </button>
      }
    >
      <div className="split-admin">
        <form className="compact-form" onSubmit={submit}>
          <h3>PDA 번호 추가</h3>
          <label>
            표시번호
            <input
              type="number"
              min="1"
              value={num}
              onChange={(e)=>setNum(e.target.value)}
              required
            />
          </label>
          <button className="primary-button">기기 등록</button>
          <small>
            표시번호와 내부 PK는 분리됩니다. 과거 사용이력은 표시번호를 변경해도 같은 기기를 따라갑니다.
          </small>
        </form>

        <div>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>교체</th>
                  <th>PK</th>
                  <th>표시번호</th>
                  <th>상태</th>
                  <th>번호 수정</th>
                  <th>사용이력</th>
                  <th>삭제/폐기</th>
                </tr>
              </thead>
              <tbody>
                {pdas.map((pda)=>(
                  <tr key={pda.id} className={historyDevice?.id===pda.id?'selected-row':''}>
                    <td>
                      <input
                        className="tiny-check"
                        type="checkbox"
                        checked={checked.includes(pda.id)}
                        onChange={()=>toggle(pda.id)}
                      />
                    </td>
                    <td>#{pda.id}</td>
                    <td><strong>{pda.deviceNumber}</strong></td>
                    <td>
                      <select
                        className="cell-select"
                        value={pda.status}
                        onChange={(e)=>
                          void api.updatePdaStatus(
                            pda.id,
                            e.target.value as Pda['status']
                          ).then(reload)
                        }
                      >
                        {['AVAILABLE','IN_USE','LOST','INSPECTION','RETIRED'].map((s)=>
                          <option key={s}>{s}</option>
                        )}
                      </select>
                    </td>
                    <td>
                      <div className="inline-edit">
                        <input
                          type="number"
                          min="1"
                          value={edit[pda.id]??''}
                          placeholder={String(pda.deviceNumber)}
                          onChange={(e)=>setEdit((v)=>({...v,[pda.id]:e.target.value}))}
                        />
                        <button onClick={()=>void changeNumber(pda)} title="번호 저장">
                          <Save size={13}/>
                        </button>
                      </div>
                    </td>
                    <td>
                      <div className="erp-cell-actions">
                        <button className="erp-row-button" onClick={()=>void openHistory(pda)}>
                          <History size={13}/> 사용
                        </button>
                        <Link className="erp-row-button" to={`/audit-logs?referenceType=PDA_DEVICE&referenceId=${pda.id}`}>
                          <History size={13}/> 감사
                        </Link>
                      </div>
                    </td>
                    <td>
                      <button className="icon-danger" onClick={()=>void remove(pda)}>
                        <Trash2 size={14}/>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {historyDevice && (
            <div className="erp-subtable-block">
              <div className="erp-subtable-title">
                <strong>PDA #{historyDevice.id} / 표시번호 {historyDevice.deviceNumber}</strong>
                <span>사용 이력 {history.length}건</span>
              </div>
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>사용자</th>
                      <th>사원번호</th>
                      <th>할당시각</th>
                      <th>반납시각</th>
                      <th>종료사유</th>
                    </tr>
                  </thead>
                  <tbody>
                    {history.map((row)=>(
                      <tr key={row.usageId}>
                        <td><strong>{row.nickname}</strong></td>
                        <td>{row.employeeNo}</td>
                        <td>{fmt(row.assignedAt)}</td>
                        <td>{fmt(row.releasedAt)}</td>
                        <td>{row.releaseReason??(row.releasedAt?'':'사용중')}</td>
                      </tr>
                    ))}
                    {history.length===0&&(
                      <tr><td colSpan={5} className="empty-cell">사용 이력이 없습니다.</td></tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      </div>
    </Panel>
  )
}

function LocationSettings({locations,reload}:{locations:Location[];reload:()=>Promise<void>}) {
  const [alphabet,setAlphabet]=useState('A')
  const [start,setStart]=useState('1')
  const [end,setEnd]=useState('3')
  const [floor,setFloor]=useState('1')

  const [selected,setSelected]=useState<number|null>(null)
  const [child,setChild]=useState('')
  const [sibling,setSibling]=useState('')
  const [rangeStart,setRangeStart]=useState('1')
  const [rangeEnd,setRangeEnd]=useState('5')

  const current=locations.find((l)=>l.id===selected)??null
  const roots=locations.filter((l)=>l.parentId===null)

  const [metaFloor,setMetaFloor]=useState('')
  const [metaFood,setMetaFood]=useState<'NON_FOOD'|'FOOD'>('NON_FOOD')
  const [metaCategories,setMetaCategories]=useState<string[]>([])

  useEffect(()=>{
    if(!current)return
    setMetaFloor(current.floor==null?'':String(current.floor))
    setMetaFood(current.foodType)
    setMetaCategories(current.nonFoodCategories)
  },[current?.id])

  const submit=async(e:React.FormEvent)=>{
    e.preventDefault()
    await api.createAreaRange({
      alphabet,
      startNumber:Number(start),
      endNumber:Number(end),
      width:2,
      floor:Number(floor),
      foodType:'NON_FOOD',
      nonFoodCategories:['GENERAL']
    })
    await reload()
  }

  const addChild=async()=>{
    if(!selected||!child)return
    await api.addLocationChild(selected,child)
    setChild('')
    await reload()
  }

  const addSibling=async()=>{
    if(!selected||!sibling)return
    await api.addLocationSibling(selected,sibling)
    setSibling('')
    await reload()
  }

  const addRange=async()=>{
    if(!selected)return
    await api.addLocationRange(
      selected,
      Number(rangeStart),
      Number(rangeEnd),
      2
    )
    await reload()
  }

  const toggleCategory=(category:string)=>{
    setMetaCategories((prev)=>
      prev.includes(category)
        ? prev.filter((x)=>x!==category)
        : [...prev,category]
    )
  }

  const saveMeta=async()=>{
    if(!current)return
    await api.updateLocationMetadata(current.id,{
      floor:metaFloor===''?null:Number(metaFloor),
      foodType:metaFood,
      nonFoodCategories:metaFood==='NON_FOOD'?metaCategories:[]
    })
    await reload()
  }

  return (
    <Panel title="로케이션 관리">
      <div className="location-admin-layout">
        <div className="location-left">
          <form className="compact-form" onSubmit={submit}>
            <h3>최상위 구역 일괄 생성</h3>
            <div className="form-grid-2">
              <label>알파벳<input value={alphabet} onChange={(e)=>setAlphabet(e.target.value.toUpperCase())}/></label>
              <label>층수<input type="number" value={floor} onChange={(e)=>setFloor(e.target.value)}/></label>
              <label>시작번호<input type="number" value={start} onChange={(e)=>setStart(e.target.value)}/></label>
              <label>종료번호<input type="number" value={end} onChange={(e)=>setEnd(e.target.value)}/></label>
            </div>
            <button className="primary-button">A01~ 생성</button>
          </form>

          <div className="location-tree">
            {roots.map((r)=>
              <LocationTreeItem
                key={r.id}
                item={r}
                all={locations}
                selected={selected}
                setSelected={setSelected}
              />
            )}
          </div>
        </div>

        <div className="location-editor">
          {current?(
            <>
              <div className="erp-editor-title">
                <span className="eyebrow">SELECTED LOCATION</span>
                <h3>{current.fullCode}</h3>
                <small>PK #{current.id} · DEPTH {current.depth}</small>
                <Link className="erp-row-button erp-audit-inline-link" to={`/audit-logs?referenceType=LOCATION&referenceId=${current.id}`}>
                  <History size={12}/> 변경이력
                </Link>
              </div>

              <div className="erp-fieldset">
                <h4>속성</h4>
                <div className="erp-form-row">
                  <label>
                    층수
                    <input
                      type="number"
                      value={metaFloor}
                      onChange={(e)=>setMetaFloor(e.target.value)}
                    />
                  </label>

                  <label>
                    구분
                    <select
                      value={metaFood}
                      onChange={(e)=>setMetaFood(e.target.value as 'NON_FOOD'|'FOOD')}
                    >
                      <option value="NON_FOOD">비식품</option>
                      <option value="FOOD">식품</option>
                    </select>
                  </label>
                </div>

                {metaFood==='NON_FOOD'&&(
                  <div className="erp-checkbox-grid">
                    {[
                      ['GENERAL','일반'],
                      ['COLOR','컬러'],
                      ['HYGIENE','위생'],
                      ['TOOLS','공구']
                    ].map(([value,label])=>(
                      <label key={value}>
                        <input
                          type="checkbox"
                          checked={metaCategories.includes(value)}
                          onChange={()=>toggleCategory(value)}
                        />
                        {label}
                      </label>
                    ))}
                  </div>
                )}

                <button className="primary-button compact" onClick={()=>void saveMeta()}>
                  <Save size={13}/> 속성 저장
                </button>
              </div>

              <div className="erp-fieldset">
                <h4>계층 확장</h4>
                <label>
                  하위 단계 직접 추가
                  <div className="inline-edit">
                    <input
                      value={child}
                      onChange={(e)=>setChild(e.target.value.toUpperCase())}
                      placeholder="05"
                    />
                    <button onClick={()=>void addChild()}><Plus size={13}/></button>
                  </div>
                </label>

                <label>
                  같은 단계 추가
                  <div className="inline-edit">
                    <input
                      value={sibling}
                      onChange={(e)=>setSibling(e.target.value.toUpperCase())}
                      placeholder="25"
                    />
                    <button onClick={()=>void addSibling()}><Plus size={13}/></button>
                  </div>
                </label>

                <label>
                  하위 숫자 범위 추가
                  <div className="range-inline">
                    <input type="number" value={rangeStart} onChange={(e)=>setRangeStart(e.target.value)}/>
                    <span>~</span>
                    <input type="number" value={rangeEnd} onChange={(e)=>setRangeEnd(e.target.value)}/>
                    <button onClick={()=>void addRange()}>추가</button>
                  </div>
                </label>
              </div>

              <button
                className="danger-text-button"
                onClick={()=>void api.deactivateLocation(current.id).then(reload)}
              >
                비활성 처리
              </button>
            </>
          ):(
            <div className="empty">왼쪽 트리에서 로케이션을 선택하세요.</div>
          )}
        </div>
      </div>
    </Panel>
  )
}

function LocationTreeItem({item,all,selected,setSelected}:{item:Location;all:Location[];selected:number|null;setSelected:(id:number)=>void}) {
  const kids=all.filter((l)=>l.parentId===item.id)
  return <div className="tree-node"><button className={selected===item.id?'selected':''} onClick={()=>setSelected(item.id)}>{item.fullCode}</button>{kids.length>0&&<div className="tree-children">{kids.map((k)=><LocationTreeItem key={k.id} item={k} all={all} selected={selected} setSelected={setSelected}/>)}</div>}</div>
}

function WorkTypeSettings({items,reload}:{items:WorkType[];reload:()=>Promise<void>}) {
  const [name,setName]=useState('')
  const [description,setDescription]=useState('')
  const [selected,setSelected]=useState<WorkType|null>(null)
  const [editName,setEditName]=useState('')
  const [editDescription,setEditDescription]=useState('')

  const submit=async(e:React.FormEvent)=>{
    e.preventDefault()
    await api.createWorkType({name,description})
    setName('')
    setDescription('')
    await reload()
  }

  const select=(item:WorkType)=>{
    setSelected(item)
    setEditName(item.name)
    setEditDescription(item.description??'')
  }

  const save=async()=>{
    if(!selected)return
    await api.updateWorkType(selected.id,{
      name:editName,
      description:editDescription
    })
    setSelected(null)
    await reload()
  }

  const deactivate=async(item:WorkType)=>{
    if(!confirm(`${item.name} 업무 종류를 비활성 처리할까요?`))return
    await api.deactivateWorkType(item.id)
    if(selected?.id===item.id)setSelected(null)
    await reload()
  }

  return (
    <Panel title="업무 종류 관리">
      <div className="split-admin">
        <form className="compact-form" onSubmit={submit}>
          <h3>신규 업무 등록</h3>
          <label>
            업무명
            <input value={name} onChange={(e)=>setName(e.target.value)} required/>
          </label>
          <label>
            설명
            <textarea value={description} onChange={(e)=>setDescription(e.target.value)}/>
          </label>
          <button className="primary-button">등록</button>
        </form>

        <div>
          <div className="table-wrap">
            <table>
              <thead>
                <tr><th>업무명</th><th>설명</th><th>상태</th><th>관리</th></tr>
              </thead>
              <tbody>
                {items.map((item)=>(
                  <tr key={item.id}>
                    <td><strong>{item.name}</strong></td>
                    <td>{item.description??'-'}</td>
                    <td>{item.active?'ACTIVE':'INACTIVE'}</td>
                    <td>
                      <div className="erp-cell-actions">
                        <button onClick={()=>select(item)}><Pencil size={13}/> 수정</button>
                        <Link className="erp-row-button" to={`/audit-logs?referenceType=WORK_TYPE&referenceId=${item.id}`}><History size={12}/> 감사</Link>
                        {item.active&&(
                          <button onClick={()=>void deactivate(item)}><Trash2 size={13}/> 비활성</button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {selected&&(
            <div className="erp-inline-editor">
              <strong>업무 종류 수정 · #{selected.id}</strong>
              <label>업무명<input value={editName} onChange={(e)=>setEditName(e.target.value)}/></label>
              <label>설명<input value={editDescription} onChange={(e)=>setEditDescription(e.target.value)}/></label>
              <div className="erp-editor-actions">
                <button className="secondary-button" onClick={()=>setSelected(null)}>취소</button>
                <button className="primary-button" onClick={()=>void save()}><Save size={13}/> 저장</button>
              </div>
            </div>
          )}
        </div>
      </div>
    </Panel>
  )
}

function IssueTypeSettings({items,reload}:{items:IssueType[];reload:()=>Promise<void>}) {
  const [name,setName]=useState('')
  const [loc,setLoc]=useState(false)
  const [product,setProduct]=useState(false)
  const [qty,setQty]=useState(false)

  const [selected,setSelected]=useState<IssueType|null>(null)
  const [editName,setEditName]=useState('')
  const [editLoc,setEditLoc]=useState(false)
  const [editProduct,setEditProduct]=useState(false)
  const [editQty,setEditQty]=useState(false)

  const submit=async(e:React.FormEvent)=>{
    e.preventDefault()
    await api.createIssueType({
      name,
      requireLocation:loc,
      requireProductCode:product,
      requireQuantity:qty
    })
    setName('')
    setLoc(false)
    setProduct(false)
    setQty(false)
    await reload()
  }

  const select=(item:IssueType)=>{
    setSelected(item)
    setEditName(item.name)
    setEditLoc(item.requireLocation)
    setEditProduct(item.requireProductCode)
    setEditQty(item.requireQuantity)
  }

  const save=async()=>{
    if(!selected)return
    await api.updateIssueType(selected.id,{
      name:editName,
      requireLocation:editLoc,
      requireProductCode:editProduct,
      requireQuantity:editQty
    })
    setSelected(null)
    await reload()
  }

  const deactivate=async(item:IssueType)=>{
    if(!confirm(`${item.name} 특이사항 구분을 비활성 처리할까요?`))return
    await api.deactivateIssueType(item.id)
    if(selected?.id===item.id)setSelected(null)
    await reload()
  }

  const requirement=(item:IssueType)=>
    [
      item.requireLocation&&'로케이션',
      item.requireProductCode&&'상품코드',
      item.requireQuantity&&'수량'
    ].filter(Boolean).join(' · ')||'코멘트만'

  return (
    <Panel title="특이사항 구분 / 구성요건">
      <div className="split-admin">
        <form className="compact-form" onSubmit={submit}>
          <h3>신규 구분 등록</h3>
          <label>
            구분명
            <input value={name} onChange={(e)=>setName(e.target.value)} required/>
          </label>
          <div className="check-stack">
            <label><input type="checkbox" checked={loc} onChange={(e)=>setLoc(e.target.checked)}/>로케이션</label>
            <label><input type="checkbox" checked={product} onChange={(e)=>setProduct(e.target.checked)}/>상품코드</label>
            <label><input type="checkbox" checked={qty} onChange={(e)=>setQty(e.target.checked)}/>수량</label>
          </div>
          <button className="primary-button">등록</button>
        </form>

        <div>
          <div className="table-wrap">
            <table>
              <thead>
                <tr><th>구분명</th><th>필수 구성요건</th><th>상태</th><th>관리</th></tr>
              </thead>
              <tbody>
                {items.map((item)=>(
                  <tr key={item.id}>
                    <td><strong>{item.name}</strong></td>
                    <td>{requirement(item)}</td>
                    <td>{item.active?'ACTIVE':'INACTIVE'}</td>
                    <td>
                      <div className="erp-cell-actions">
                        <button onClick={()=>select(item)}><Pencil size={13}/> 수정</button>
                        <Link className="erp-row-button" to={`/audit-logs?referenceType=ISSUE_TYPE&referenceId=${item.id}`}><History size={12}/> 감사</Link>
                        {item.active&&(
                          <button onClick={()=>void deactivate(item)}><Trash2 size={13}/> 비활성</button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {selected&&(
            <div className="erp-inline-editor">
              <strong>특이사항 구분 수정 · #{selected.id}</strong>
              <label>구분명<input value={editName} onChange={(e)=>setEditName(e.target.value)}/></label>
              <div className="erp-checkbox-grid">
                <label><input type="checkbox" checked={editLoc} onChange={(e)=>setEditLoc(e.target.checked)}/>로케이션</label>
                <label><input type="checkbox" checked={editProduct} onChange={(e)=>setEditProduct(e.target.checked)}/>상품코드</label>
                <label><input type="checkbox" checked={editQty} onChange={(e)=>setEditQty(e.target.checked)}/>수량</label>
              </div>
              <div className="erp-editor-actions">
                <button className="secondary-button" onClick={()=>setSelected(null)}>취소</button>
                <button className="primary-button" onClick={()=>void save()}><Save size={13}/> 저장</button>
              </div>
            </div>
          )}
        </div>
      </div>
    </Panel>
  )
}
